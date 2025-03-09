package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserQuestionHistory;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdaptiveService {

    private final UserRepository userRepository;
    private final UserQuestionHistoryRepository historyRepository;
    private final NotificationService notificationService;

    @Value("${app.adaptive.enableIntermediateLevels:false}")
    private boolean enableIntermediateLevels;

    @Value("${app.adaptive.maxIntermediateSublevels:2}")
    private int maxIntermediateSublevels;

    public void evaluateUserProgress(Long userId) throws InvalidInputException {
        System.out.println("=== evaluateUserProgress for userId: " + userId + " ===");
        User user = userRepository.findById(userId).orElseThrow(() ->
                new InvalidInputException("User does not exist: " + userId)
        );

        List<UserQuestionHistory> lastAttempts = historyRepository.findAll()
                .stream()
                .filter(h -> h.getUser().getId().equals(userId))
                .sorted(Comparator.comparing(UserQuestionHistory::getAttemptTime).reversed())
                .limit(5)
                .toList();

        if (lastAttempts.isEmpty()) {
            System.out.println("No attempts -> no adaptation performed.");
            return;
        }

        long correctCount = lastAttempts.stream().filter(UserQuestionHistory::isCorrect).count();
        double successRate = (double) correctCount / lastAttempts.size();
        System.out.println("Success rate = " + successRate);

        DifficultyLevel oldDifficulty = (user.getCurrentDifficulty() != null)
                ? user.getCurrentDifficulty()
                : DifficultyLevel.BASIC;

        DifficultyLevel newDifficulty = oldDifficulty;

        if (successRate < 0.4) {
            DifficultyLevel lowered = getLowerDifficulty(oldDifficulty);
            if (!lowered.equals(oldDifficulty)) {
                newDifficulty = lowered;
                System.out.println("Lowering difficulty from " + oldDifficulty + " to " + newDifficulty);
                Topic recentTopic = lastAttempts.get(0).getQuestion().getTopic();
                String topicName = (recentTopic != null) ? recentTopic.getName() : "this topic";
                notificationService.notifyUserDifficulty(user.getUsername(), topicName);

            } else {
                System.out.println("Already at BASIC, cannot lower further.");
            }
        }
        else if (successRate > 0.8) {
            DifficultyLevel higher = getHigherDifficulty(oldDifficulty);
            if (!higher.equals(oldDifficulty)) {
                newDifficulty = higher;
                System.out.println("Raising difficulty from " + oldDifficulty + " to " + newDifficulty);

                Topic recentTopic = lastAttempts.get(0).getQuestion().getTopic();
                String topicName = (recentTopic != null) ? recentTopic.getName() : "this topic";
                notificationService.notifyUserSuccess(user.getUsername(), topicName);

            } else {
                System.out.println("Already at EXPERT, cannot raise further.");
            }
        }
        else {
            System.out.println("User's success rate is moderate; no difficulty change.");
        }

        user.setCurrentDifficulty(newDifficulty);
        user.setSubDifficultyLevel(0);
        userRepository.save(user);

        System.out.println("=== Final difficulty = " + newDifficulty + " ===");
    }


    private DifficultyLevel getLowerDifficulty(DifficultyLevel d) {
        return switch (d) {
            case MEDIUM -> DifficultyLevel.EASY;
            case ADVANCED -> DifficultyLevel.MEDIUM;
            case EXPERT -> DifficultyLevel.ADVANCED;
            case EASY -> DifficultyLevel.BASIC;
            default -> DifficultyLevel.BASIC;
        };
    }

    private DifficultyLevel getHigherDifficulty(DifficultyLevel d) {
        return switch (d) {
            case BASIC -> DifficultyLevel.EASY;
            case EASY -> DifficultyLevel.MEDIUM;
            case MEDIUM -> DifficultyLevel.ADVANCED;
            case ADVANCED -> DifficultyLevel.EXPERT;
            default -> DifficultyLevel.EXPERT;
        };
    }
}

