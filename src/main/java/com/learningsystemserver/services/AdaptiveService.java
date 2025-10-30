package com.learningsystemserver.services;

import com.learningsystemserver.entities.*;
import com.learningsystemserver.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdaptiveService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final UserQuestionHistoryRepository historyRepository;
    private final UserSubtopicProgressRepository progressRepository;

    // --- Tunables (industry-style heuristic with hysteresis & cooldown) ---
    private static final int WINDOW = 10;             // rolling window size
    private static final double UP_THRESHOLD = 0.75;  // promote threshold in window
    private static final double DOWN_THRESHOLD = 0.45;// demote threshold in window
    private static final int UP_STREAK = 3;           // require 3 in a row to move up
    private static final int DOWN_STREAK = 2;         // require 2 in a row to move down
    private static final int COOLDOWN_ATTEMPTS = 3;   // minimal attempts between changes

    @Transactional(readOnly = true)
    public DifficultyLevel getDifficultyForSubtopic(Long userId, Long subtopicId) {
        return progressRepository.findByUserIdAndSubtopicId(userId, subtopicId)
                .map(UserSubtopicProgress::getCurrentDifficulty)
                .orElse(DifficultyLevel.BASIC);
    }

    /**
     * Called after an attempt is saved. Evaluates ONLY the subtopic just practiced.
     */
    @Transactional
    public void evaluateUserProgress(Long userId, Long subtopicId) {
        User user = userRepository.findById(userId).orElseThrow();
        Topic subtopic = topicRepository.findById(subtopicId).orElseThrow();

        UserSubtopicProgress progress = progressRepository
                .findByUserIdAndSubtopicId(userId, subtopicId)
                .orElseGet(() -> progressRepository.save(
                        UserSubtopicProgress.builder()
                                .user(user)
                                .subtopic(subtopic)
                                .currentDifficulty(DifficultyLevel.BASIC)
                                .correctStreak(0)
                                .wrongStreak(0)
                                .attemptsSinceLastChange(0)
                                .lastUpdatedAt(LocalDateTime.now())
                                .build()
                ));

        // Window of last attempts for this subtopic
        List<UserQuestionHistory> last = historyRepository
                .findTop10ByUserIdAndQuestion_Topic_IdOrderByAttemptTimeDesc(userId, subtopicId);

        if (last.isEmpty()) {
            // nothing to evaluate yet
            return;
        }

        boolean lastCorrect = last.get(0).isCorrect();
        if (lastCorrect) {
            progress.setCorrectStreak(progress.getCorrectStreak() + 1);
            progress.setWrongStreak(0);
        } else {
            progress.setWrongStreak(progress.getWrongStreak() + 1);
            progress.setCorrectStreak(0);
        }

        int attemptsSinceLastChange = progress.getAttemptsSinceLastChange() + 1;

        long correct = last.stream().filter(UserQuestionHistory::isCorrect).count();
        double rate = (double) correct / last.size();

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

        // Backward-compat: keep user's legacy 'currentDifficulty' in sync with what they just practiced.
        // (Existing UI pieces that still read this will keep working.)
        if (user.getCurrentDifficulty() != after) {
            user.setCurrentDifficulty(after);
            user.setSubDifficultyLevel(0);
            userRepository.save(user);
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

    private DifficultyLevel bumpUp(DifficultyLevel d) {
        return ORDER.entrySet().stream()
                .filter(e -> e.getValue() == Math.min(5, ORDER.get(d) + 1))
                .map(Map.Entry::getKey)
                .findFirst().orElse(d);
    }

    private DifficultyLevel bumpDown(DifficultyLevel d) {
        return ORDER.entrySet().stream()
                .filter(e -> e.getValue() == Math.max(1, ORDER.get(d) - 1))
                .map(Map.Entry::getKey)
                .findFirst().orElse(d);
    }
}
