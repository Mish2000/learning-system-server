package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserQuestionHistory;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import static com.learningsystemserver.exceptions.ErrorMessages.USER_ID_DOES_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class AdaptiveService {

    private final UserRepository userRepository;
    private final UserQuestionHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public void evaluateUserProgress(Long userId) throws InvalidInputException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(USER_ID_DOES_NOT_EXIST.getMessage(), userId)
                ));

        List<UserQuestionHistory> lastAttempts = historyRepository.findAll()
                .stream()
                .filter(h -> h.getUser().getId().equals(userId))
                .sorted(Comparator.comparing(UserQuestionHistory::getAttemptTime).reversed())
                .limit(5)
                .toList();

        if (lastAttempts.size() < 5) {
            return;
        }

        long correctCount = lastAttempts.stream().filter(UserQuestionHistory::isCorrect).count();
        double successRate = (double) correctCount / lastAttempts.size();

        DifficultyLevel currentDifficulty = user.getCurrentDifficulty();
        if (currentDifficulty == null) {
            currentDifficulty = DifficultyLevel.BASIC;
        }

        if (successRate < 0.4) {
            DifficultyLevel newDifficulty = getLowerDifficulty(currentDifficulty);
            user.setCurrentDifficulty(newDifficulty);


            Topic recentTopic = lastAttempts.get(0).getQuestion().getTopic();
            String topicName = (recentTopic != null) ? recentTopic.getName() : "this topic";
            notificationService.notifyUserDifficulty(user.getUsername(), topicName);

        } else if (successRate > 0.8) {
            DifficultyLevel newDifficulty = getHigherDifficulty(currentDifficulty);
            user.setCurrentDifficulty(newDifficulty);
        }

        userRepository.save(user);
    }

    private DifficultyLevel getLowerDifficulty(DifficultyLevel d) {
        return switch (d) {
            case MEDIUM -> DifficultyLevel.EASY;
            case ADVANCED -> DifficultyLevel.MEDIUM;
            case EXPERT -> DifficultyLevel.ADVANCED;
            default -> DifficultyLevel.BASIC;
        };
    }

    private DifficultyLevel getHigherDifficulty(DifficultyLevel d) {
        return switch (d) {
            case BASIC -> DifficultyLevel.EASY;
            case EASY -> DifficultyLevel.MEDIUM;
            case MEDIUM -> DifficultyLevel.ADVANCED;
            default -> DifficultyLevel.EXPERT;
        };
    }
}

