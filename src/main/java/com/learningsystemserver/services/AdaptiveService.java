package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserQuestionHistory;
import com.learningsystemserver.entities.UserSubtopicProgress;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.repositories.UserSubtopicProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdaptiveService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final UserQuestionHistoryRepository historyRepository;
    private final UserSubtopicProgressRepository progressRepository;
    private final NotificationService notificationService;

    // --- Tunables (rolling window, thresholds, hysteresis & cooldown) ---
    private static final int WINDOW = 10;             // rolling window size (actively used)
    private static final double UP_THRESHOLD = 0.75;  // promote threshold in window
    private static final double DOWN_THRESHOLD = 0.45;// demote threshold in window
    private static final int UP_STREAK = 3;           // require 3 in a row to move up
    private static final int DOWN_STREAK = 2;         // require 2 in a row to move down
    private static final int COOLDOWN_ATTEMPTS = 3;   // minimal attempts between changes

    @Transactional
    public void evaluateUserProgress(Long userId, Long subtopicId) {
        User user = userRepository.findById(userId).orElseThrow();
        Topic subtopic = topicRepository.findById(subtopicId).orElseThrow();

        // Ensure we have a progress row (INIT = BASIC; no dependency on user.currentDifficulty)
        UserSubtopicProgress progress = progressRepository
                .findByUserIdAndSubtopicId(userId, subtopicId)
                .orElseGet(() -> progressRepository.save(
                        UserSubtopicProgress.builder()
                                .user(user)
                                .subtopic(subtopic)
                                .currentDifficulty(DifficultyLevel.BASIC) // CHANGED
                                .correctStreak(0)
                                .wrongStreak(0)
                                .attemptsSinceLastChange(0)
                                .lastUpdatedAt(LocalDateTime.now())
                                .build()
                ));

        List<UserQuestionHistory> last = historyRepository
                .findTop10ByUserIdAndQuestion_Topic_IdOrderByAttemptTimeDesc(userId, subtopicId);
        if (last.isEmpty()) return;

        List<UserQuestionHistory> recent = last.subList(0, Math.min(last.size(), WINDOW));

        boolean lastCorrect = recent.get(0).isCorrect();
        if (lastCorrect) {
            progress.setCorrectStreak(progress.getCorrectStreak() + 1);
            progress.setWrongStreak(0);
        } else {
            progress.setWrongStreak(progress.getWrongStreak() + 1);
            progress.setCorrectStreak(0);
        }

        int attemptsSinceLastChange = progress.getAttemptsSinceLastChange() + 1;

        long correctCount = recent.stream().filter(UserQuestionHistory::isCorrect).count();
        double rate = correctCount / (double) recent.size();

        DifficultyLevel before = progress.getCurrentDifficulty();
        DifficultyLevel after = before;

        boolean canChange = attemptsSinceLastChange >= COOLDOWN_ATTEMPTS;

        if (canChange && rate >= UP_THRESHOLD && progress.getCorrectStreak() >= UP_STREAK) {
            after = bumpUp(before);
            attemptsSinceLastChange = 0;
        } else if (canChange && rate <= DOWN_THRESHOLD && progress.getWrongStreak() >= DOWN_STREAK) {
            after = bumpDown(before);
            attemptsSinceLastChange = 0;
        }

        progress.setCurrentDifficulty(after);
        progress.setAttemptsSinceLastChange(attemptsSinceLastChange);
        progress.setLastUpdatedAt(LocalDateTime.now());
        progressRepository.save(progress);

        // REMOVED legacy sync to user.currentDifficulty

        if (after != before) {
            boolean promoted = ORDER.get(after) > ORDER.get(before);
            String message = String.format(
                    "Your difficulty for %s %s from %s to %s.",
                    subtopic.getName(),
                    promoted ? "increased" : "decreased",
                    before.name(),
                    after.name()
            );
            notificationService.createNotification(
                    message,
                    user.getUsername(),
                    promoted ? "USER_SUCCESS" : "USER_WARNING"
            );
        }
    }


    // --- Helper mapping for DifficultyLevel order ---
    private static final Map<DifficultyLevel, Integer> ORDER = Map.of(
            DifficultyLevel.BASIC, 1,
            DifficultyLevel.EASY, 2,
            DifficultyLevel.MEDIUM, 3,
            DifficultyLevel.ADVANCED, 4,
            DifficultyLevel.EXPERT, 5
    );

    private static DifficultyLevel bumpUp(DifficultyLevel d) {
        int next = Math.min(5, ORDER.get(d) + 1);
        for (Entry<DifficultyLevel, Integer> e : ORDER.entrySet()) {
            if (e.getValue() == next) return e.getKey();
        }
        return d;
    }

    private static DifficultyLevel bumpDown(DifficultyLevel d) {
        int prev = Math.max(1, ORDER.get(d) - 1);
        for (Entry<DifficultyLevel, Integer> e : ORDER.entrySet()) {
            if (e.getValue() == prev) return e.getKey();
        }
        return d;
    }

    public record DifficultyChange(Long topicId,
                                   String topicName,
                                   DifficultyLevel before,
                                   DifficultyLevel after,
                                   boolean promoted) {}

    // Pretty-print enum names: BASIC -> Basic, etc.
    private static String pretty(DifficultyLevel level) {
        String s = level.name().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

