package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.requests.QuestionRequest;
import com.learningsystemserver.dtos.requests.SubmitAnswerRequest;
import com.learningsystemserver.dtos.responses.QuestionResponse;
import com.learningsystemserver.dtos.responses.SubmitAnswerResponse;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.QuestionGeneratorService;
import com.learningsystemserver.services.UserHistoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.learningsystemserver.exceptions.ErrorMessages.USERNAME_DOES_NOT_EXIST;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionGeneratorService questionService;
    private final UserHistoryService userHistoryService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    public QuestionResponse generateQuestion(@RequestBody QuestionRequest request) {
        DifficultyLevel level = request.getDifficultyLevel();
        if (level == null) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("No user found with username: " + username));
            level = (user.getCurrentDifficulty() != null)
                    ? user.getCurrentDifficulty()
                    : DifficultyLevel.BASIC;
        }

        GeneratedQuestion q = questionService.generateQuestion(request.getTopicId(), level);
        return toResponse(q);
    }


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
        System.out.println("userAnswer :  " + request.getUserAnswer());
        if (topicName.contains("rectangle") || topicName.contains("circle") ||
                topicName.contains("triangle") || topicName.contains("polygon")) {
            isCorrect = checkFlexibleAnswer(q.getCorrectAnswer(), request.getUserAnswer());

        } else
            isCorrect = q.getCorrectAnswer().equalsIgnoreCase(request.getUserAnswer());


        userHistoryService.logAttempt(
                user.getId(),
                q.getId(),
                isCorrect,
                request.getUserAnswer(),
                null
        );

        return new SubmitAnswerResponse(isCorrect, q.getCorrectAnswer(), q.getSolutionSteps());
    }


    private boolean checkFlexibleAnswer(String correctAnswer, String userAnswer) {

        String filteredUserAnswer = userAnswer.toLowerCase().replaceAll("\\D", " ");
        String filteredCorrectAnswer = correctAnswer.toLowerCase().replaceAll("\\D", " ");

        System.out.println("filteredCorrectAnswer"+filteredCorrectAnswer);
        String[] userAnswerFilteredArray = filteredUserAnswer.split("\\s+");
        String[] correctParts = filteredCorrectAnswer.split("\\s+");

        System.out.println("correctParts"+Arrays.toString(correctParts));

        // nonEmptyAnswers is not currently used
        String[] nonEmptyAnswers = Arrays.stream(userAnswerFilteredArray)
                .filter(userAnswerFiltered -> !userAnswerFiltered.equals(""))
                .toArray(String[]::new);

        Map<Integer,String> nonEmptyPartsMap = new HashMap<>();
        int counter = 0;
        for (int i = 0;i<correctParts.length;i++) {
            if (!correctParts[i].isEmpty()) {
                nonEmptyPartsMap.put(counter, correctParts[i]);
                counter++;
            }
        }
        nonEmptyPartsMap.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));

        Map<Integer,String> nonEmptyAnswerMap = new HashMap<>();
        counter = 0;
        for (int i = 0;i<userAnswerFilteredArray.length;i++) {
            if (!userAnswerFilteredArray[i].isEmpty()) {
                nonEmptyAnswerMap.put(counter, userAnswerFilteredArray[i]);
                counter++;
            }
        }
        nonEmptyAnswerMap.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));

        for (int i = 0; i < counter; i++) {
            System.out.println("nonEmptyUserAnswerMap "+nonEmptyAnswerMap.get(i));
            if (!nonEmptyAnswerMap.get(i).equals(nonEmptyPartsMap.get(i))) {
                System.out.println(i+"not equals");
                return false;
            }
        }
        return true;
    }

    @GetMapping("/{id}")
    public QuestionResponse getQuestion(@PathVariable Long id) throws InvalidInputException {
        GeneratedQuestion q = questionService.getQuestionById(id);
        return toResponse(q);
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

