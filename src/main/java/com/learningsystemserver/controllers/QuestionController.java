package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.requests.QuestionRequest;
import com.learningsystemserver.dtos.requests.SubmitAnswerRequest;
import com.learningsystemserver.dtos.responses.QuestionResponse;
import com.learningsystemserver.dtos.responses.SubmitAnswerResponse;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserQuestionHistory;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.QuestionGeneratorService;
import com.learningsystemserver.services.UserHistoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.learningsystemserver.exceptions.ErrorMessages.USERNAME_DOES_NOT_EXIST;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionGeneratorService questionService;
    private final UserHistoryService userHistoryService;
    private final UserRepository userRepository;
    private final UserQuestionHistoryRepository historyRepository;

    /**
     * Generate a question where difficulty is chosen adaptively per user+subtopic.
     * Manual client difficulty is ignored (side quest requirement).
     */
    @PostMapping("/generate")
    public QuestionResponse generateQuestion(@RequestBody QuestionRequest request) throws InvalidInputException {
        final Long topicId = request.getTopicId();

        final String username = SecurityContextHolder.getContext().getAuthentication().getName();
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidInputException("No user found with username: " + username));

        final DifficultyLevel effectiveLevel =
                (topicId == null)
                        ? (user.getCurrentDifficulty() != null ? user.getCurrentDifficulty() : DifficultyLevel.BASIC)
                        : resolveNextDifficultyForSubtopic(user.getId(), topicId);

        GeneratedQuestion q = questionService.generateQuestion(topicId, effectiveLevel);
        return toResponse(q);
    }

    /**
     * Submit an answer; logging is unchanged (preserves your flexible geometry checker).
     */
    @PostMapping("/submit")
    public SubmitAnswerResponse submitAnswer(@RequestBody SubmitAnswerRequest request)
            throws InvalidInputException {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("QuestionController - principalName={}", principalName);

        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(USERNAME_DOES_NOT_EXIST.getMessage(), principalName)
                ));

        GeneratedQuestion q = questionService.getQuestionById(request.getQuestionId());

        boolean isCorrect;
        String topicName = q.getTopic() != null ? q.getTopic().getName().toLowerCase() : "";
        if (topicName.contains("rectangle") || topicName.contains("circle") ||
                topicName.contains("triangle") || topicName.contains("polygon")) {
            isCorrect = checkFlexibleAnswer(q.getCorrectAnswer(), request.getUserAnswer());
        } else {
            isCorrect = q.getCorrectAnswer().equalsIgnoreCase(request.getUserAnswer());
        }

        userHistoryService.logAttempt(
                user.getId(),
                q.getId(),
                isCorrect,
                request.getUserAnswer(),
                request.getTimeTakenSeconds()
        );

        return new SubmitAnswerResponse(isCorrect, q.getCorrectAnswer(), q.getSolutionSteps());
    }

    @GetMapping("/{id}")
    public QuestionResponse getQuestion(@PathVariable Long id) throws InvalidInputException {
        GeneratedQuestion q = questionService.getQuestionById(id);
        return toResponse(q);
    }

    // ----------------- Adaptive difficulty (recent-window + streak hysteresis) -----------------

    private DifficultyLevel resolveNextDifficultyForSubtopic(Long userId, Long topicId) {
        // Pull recent attempts for this user in this (sub)topic; sort by newest using ID as fallback order.
        List<UserQuestionHistory> recent = historyRepository.findAll().stream()
                .filter(h -> h.getUser() != null && Objects.equals(h.getUser().getId(), userId))
                .filter(h -> h.getQuestion() != null
                        && h.getQuestion().getTopic() != null
                        && Objects.equals(h.getQuestion().getTopic().getId(), topicId))
                .sorted(Comparator.comparing(UserQuestionHistory::getId).reversed())
                .limit(8)
                .toList();

        if (recent.isEmpty()) return DifficultyLevel.BASIC;

        DifficultyLevel current = recent.get(0).getQuestion() != null
                ? recent.get(0).getQuestion().getDifficultyLevel()
                : DifficultyLevel.BASIC;

        long correct = recent.stream().filter(UserQuestionHistory::isCorrect).count();
        double sr = correct / (double) recent.size();

        boolean twoUp = recent.stream().limit(2).allMatch(UserQuestionHistory::isCorrect);
        boolean twoDown = recent.stream().limit(2).noneMatch(UserQuestionHistory::isCorrect);

        // Hysteresis bands to avoid ping-pong:
        // - Promote only if SR >= 0.8 AND last 2 are correct.
        // - Demote only if SR <= 0.4 AND last 2 are incorrect.
        // - Otherwise keep current.
        if (sr >= 0.80 && twoUp) {
            return stepUp(current);
        } else if (sr <= 0.40 && twoDown) {
            return stepDown(current);
        } else {
            return current;
        }
    }

    private static DifficultyLevel stepUp(DifficultyLevel d) {
        return switch (d) {
            case BASIC -> DifficultyLevel.EASY;
            case EASY -> DifficultyLevel.MEDIUM;
            case MEDIUM -> DifficultyLevel.ADVANCED;
            case ADVANCED, EXPERT -> DifficultyLevel.EXPERT;
        };
    }

    private static DifficultyLevel stepDown(DifficultyLevel d) {
        return switch (d) {
            case EXPERT -> DifficultyLevel.ADVANCED;
            case ADVANCED -> DifficultyLevel.MEDIUM;
            case MEDIUM -> DifficultyLevel.EASY;
            case EASY, BASIC -> DifficultyLevel.BASIC;
        };
    }

    // ----------------- your flexible geometry checker (unchanged) -----------------

    private boolean checkFlexibleAnswer(String correctAnswer, String userAnswer) {
        String filteredUserAnswer = userAnswer.toLowerCase().replaceAll("\\D", " ");
        String filteredCorrectAnswer = correctAnswer.toLowerCase().replaceAll("\\D", " ");

        String[] userAnswerFilteredArray = filteredUserAnswer.split("\\s+");
        String[] correctParts = filteredCorrectAnswer.split("\\s+");

        String[] nonEmptyAnswers = java.util.Arrays.stream(userAnswerFilteredArray)
                .filter(userAnswerFiltered -> !userAnswerFiltered.isEmpty())
                .toArray(String[]::new);

        java.util.Map<Integer, String> nonEmptyPartsMap = new java.util.HashMap<>();
        int counter = 0;
        for (String correctPart : correctParts) {
            if (!correctPart.isEmpty()) {
                nonEmptyPartsMap.put(counter, correctPart);
                counter++;
            }
        }

        java.util.Map<Integer, String> nonEmptyAnswerMap = new java.util.HashMap<>();
        counter = 0;
        for (String s : userAnswerFilteredArray) {
            if (!s.isEmpty()) {
                nonEmptyAnswerMap.put(counter, s);
                counter++;
            }
        }
        for (int i = 0; i < counter; i++) {
            if (!nonEmptyAnswerMap.get(i).equals(nonEmptyPartsMap.get(i))) {
                return false;
            }
        }
        return true;
    }

    private QuestionResponse toResponse(GeneratedQuestion q) {
        return new QuestionResponse(
                q.getId(),
                q.getQuestionText(),
                q.getSolutionSteps(),
                q.getCorrectAnswer(),
                (q.getTopic() != null) ? q.getTopic().getId() : null,
                (q.getDifficultyLevel() != null) ? q.getDifficultyLevel().name() : null
        );
    }
}
