package com.learningsystemserver.services;

import com.learningsystemserver.dtos.responses.AdminDashboardResponse;
import com.learningsystemserver.dtos.responses.UserDashboardResponse;
import com.learningsystemserver.entities.*;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.TopicRepository;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.repositories.UserSubtopicProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final UserQuestionHistoryRepository historyRepository;
    private final TopicRepository topicRepository;
    private final UserSubtopicProgressRepository progressRepository;


    // ------------------- Public APIs -------------------

    @Transactional(readOnly = true)
    public UserDashboardResponse buildUserDashboard(String username) throws InvalidInputException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidInputException("No user found for " + username));
        final Long userId = user.getId();

        // All user histories
        List<UserQuestionHistory> userHistories = historyRepository.findAll().stream()
                .filter(h -> h.getUser() != null && Objects.equals(h.getUser().getId(), userId))
                .collect(Collectors.toList());

        // Aggregates
        TopicAgg topicAgg = aggregateTopicStats(userHistories);
        long totalAttempts = userHistories.size();
        long correct = userHistories.stream().filter(UserQuestionHistory::isCorrect).count();
        double successRatePercent = totalAttempts == 0 ? 0.0 : round1((correct * 100.0) / totalAttempts);

        // Difficulty maps computed per-user from history
        TopicDifficultyMaps difficultyMaps = computeUserDifficultyMaps(userId, userHistories);

        UserDashboardResponse resp = new UserDashboardResponse();
        resp.setUserId(user.getId());
        resp.setTotalAttempts(totalAttempts);
        resp.setCorrectAttempts(correct);
        resp.setSuccessRate(successRatePercent);

        resp.setAttemptsByTopic(topicAgg.attemptsByTopic);
        resp.setSuccessRateByTopic(topicAgg.successRateByTopic);

        resp.setCurrentDifficulty(
                (user.getOverallProgressLevel() == null ? DifficultyLevel.BASIC : user.getOverallProgressLevel()).name()
        );
        resp.setSubDifficultyLevel(user.getSubDifficultyLevel());

        // live, per-user maps:
        resp.setTopicDifficulty(difficultyMaps.parentTopicDifficulty);
        resp.setSubtopicDifficulty(difficultyMaps.subtopicDifficulty);

        resp.setOverallProgressLevel(
                (user.getOverallProgressLevel() == null ? DifficultyLevel.BASIC : user.getOverallProgressLevel()).name()
        );
        resp.setOverallProgressScore(
                user.getOverallProgressScore() == null ? 1.0 : round1(user.getOverallProgressScore())
        );

        return resp;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse buildAdminDashboard() {
        List<UserQuestionHistory> all = historyRepository.findAll();

        long totalAttempts = all.size();
        long correct = all.stream().filter(UserQuestionHistory::isCorrect).count();
        double successRatePercent = totalAttempts == 0 ? 0.0 : round1((correct * 100.0) / totalAttempts);

        // De-duplicated via helpers
        Map<String, Long> attemptsByTopic = computeAttemptsByTopic(all);
        Map<String, Double> successRateByTopic = computeSuccessRateByTopic(all);

        AdminDashboardResponse resp = new AdminDashboardResponse();
        resp.setTotalUsers(userRepository.count());
        resp.setTotalAttempts(totalAttempts);
        resp.setOverallSuccessRate(successRatePercent);
        resp.setAttemptsByTopic(attemptsByTopic);
        resp.setSuccessRateByTopic(successRateByTopic);
        return resp;
    }

    // ------------------- Difficulty maps (per-user) -------------------

    private TopicDifficultyMaps computeUserDifficultyMaps(Long userId, List<UserQuestionHistory> userHistories) {
        List<Topic> allTopics = topicRepository.findAll();

        Map<Long, Topic> byId = allTopics.stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));

        // Children by parent
        Map<Long, List<Topic>> childrenByParent = allTopics.stream()
                .filter(t -> t.getParentTopic() != null)
                .collect(Collectors.groupingBy(t -> t.getParentTopic().getId()));

        // === NEW: get live per-subtopic difficulty from progress table ===
        Map<Long, DifficultyLevel> liveBySubtopicId = progressRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getSubtopic().getId(), UserSubtopicProgress::getCurrentDifficulty));

        Map<String, String> subtopicDifficulty = allTopics.stream()
                .filter(t -> t.getParentTopic() != null) // only subtopics
                .collect(Collectors.toMap(
                        Topic::getName,
                        t -> liveBySubtopicId.getOrDefault(t.getId(), DifficultyLevel.BASIC).name(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Parent difficulty = average child indices → nearest level
        Map<String, String> parentTopicDifficulty = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Topic>> e : childrenByParent.entrySet()) {
            Topic parent = byId.get(e.getKey());
            if (parent == null) continue;

            List<String> childLevels = e.getValue().stream()
                    .map(Topic::getName)
                    .map(subtopicDifficulty::get)
                    .filter(Objects::nonNull)
                    .toList();

            float avgIndex = childLevels.isEmpty()
                    ? levelIndex(DifficultyLevel.BASIC)
                    : (float) childLevels.stream()
                    .map(DifficultyLevel::valueOf)
                    .mapToInt(this::levelIndex)
                    .average()
                    .orElse(levelIndex(DifficultyLevel.BASIC));

            parentTopicDifficulty.put(parent.getName(), indexToLevel(avgIndex).name());
        }

        return new TopicDifficultyMaps(parentTopicDifficulty, subtopicDifficulty);
    }


    // ------------------- Aggregation helpers (de-duplicated) -------------------

    private TopicAgg aggregateTopicStats(List<UserQuestionHistory> list) {
        return new TopicAgg(
                computeAttemptsByTopic(list),
                computeSuccessRateByTopic(list)
        );
    }

    private Map<String, Long> computeAttemptsByTopic(List<UserQuestionHistory> list) {
        return list.stream()
                .filter(this::hasTopic)
                .collect(Collectors.groupingBy(
                        h -> h.getQuestion().getTopic().getName(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Double> computeSuccessRateByTopic(List<UserQuestionHistory> list) {
        return list.stream()
                .filter(this::hasTopic)
                .collect(Collectors.groupingBy(
                        h -> h.getQuestion().getTopic().getName(),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), attempts -> {
                            long correct = attempts.stream().filter(UserQuestionHistory::isCorrect).count();
                            return attempts.isEmpty() ? 0.0 : round1((correct * 100.0) / (double) attempts.size());
                        })
                ));
    }

    private boolean hasTopic(UserQuestionHistory h) {
        return h.getQuestion() != null && h.getQuestion().getTopic() != null;
    }

    // ------------------- Difficulty helpers -------------------

    private DifficultyLevel applyHysteresis(List<UserQuestionHistory> recent) {
        if (recent == null || recent.isEmpty()) return DifficultyLevel.BASIC;

        DifficultyLevel current = recent.get(0).getQuestion() != null
                ? recent.get(0).getQuestion().getDifficultyLevel()
                : DifficultyLevel.BASIC;

        long correct = recent.stream().filter(UserQuestionHistory::isCorrect).count();
        double sr = correct / (double) recent.size();
        boolean twoUp = recent.stream().limit(2).allMatch(UserQuestionHistory::isCorrect);
        boolean twoDown = recent.stream().limit(2).noneMatch(UserQuestionHistory::isCorrect);

        if (sr >= 0.80 && twoUp) return stepUp(current);
        if (sr <= 0.40 && twoDown) return stepDown(current);
        return current;
    }

    private int levelIndex(DifficultyLevel d) {
        return switch (d) {
            case BASIC -> 0;
            case EASY -> 1;
            case MEDIUM -> 2;
            case ADVANCED -> 3;
            case EXPERT -> 4;
        };
    }

    private DifficultyLevel indexToLevel(float idx) {
        if (idx <= 0.5f) return DifficultyLevel.BASIC;
        if (idx <= 1.5f) return DifficultyLevel.EASY;
        if (idx <= 2.5f) return DifficultyLevel.MEDIUM;
        if (idx <= 3.5f) return DifficultyLevel.ADVANCED;
        return DifficultyLevel.EXPERT;
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

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ------------------- DTOs -------------------

    private static final class TopicAgg {
        final Map<String, Long> attemptsByTopic;
        final Map<String, Double> successRateByTopic;
        TopicAgg(Map<String, Long> attemptsByTopic, Map<String, Double> successRateByTopic) {
            this.attemptsByTopic = attemptsByTopic;
            this.successRateByTopic = successRateByTopic;
        }
    }

    private static final class TopicDifficultyMaps {
        final Map<String, String> parentTopicDifficulty;
        final Map<String, String> subtopicDifficulty;
        TopicDifficultyMaps(Map<String, String> parentTopicDifficulty, Map<String, String> subtopicDifficulty) {
            this.parentTopicDifficulty = parentTopicDifficulty;
            this.subtopicDifficulty = subtopicDifficulty;
        }
    }
}
