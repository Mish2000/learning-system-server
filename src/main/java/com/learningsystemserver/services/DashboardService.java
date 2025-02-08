package com.learningsystemserver.services;

import com.learningsystemserver.dtos.responses.AdminDashboardResponse;
import com.learningsystemserver.dtos.responses.UserDashboardResponse;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserQuestionHistory;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.learningsystemserver.exceptions.ErrorMessages.USERNAME_DOES_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final UserQuestionHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public UserDashboardResponse buildUserDashboard(String username) throws InvalidInputException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidInputException(
                        String.format(USERNAME_DOES_NOT_EXIST.getMessage(), username)
                ));

        List<UserQuestionHistory> attempts = historyRepository.findAll()
                .stream()
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .toList();

        long totalAttempts = attempts.size();
        long correctAttempts = attempts.stream().filter(UserQuestionHistory::isCorrect).count();
        double successRate = (totalAttempts == 0) ? 0.0 : (double) correctAttempts / totalAttempts;

        Map<String, Long> attemptsByTopic = new HashMap<>();
        Map<String, Long> correctByTopic = new HashMap<>();

        for (UserQuestionHistory h : attempts) {
            String topicName = "Unknown";
            if (h.getQuestion().getTopic() != null) {
                topicName = h.getQuestion().getTopic().getName();
            }
            attemptsByTopic.put(topicName, attemptsByTopic.getOrDefault(topicName, 0L) + 1);

            if (h.isCorrect()) {
                correctByTopic.put(topicName, correctByTopic.getOrDefault(topicName, 0L) + 1);
            }
        }

        Map<String, Double> successRateByTopic = new HashMap<>();
        for (String topic : attemptsByTopic.keySet()) {
            long correctCount = correctByTopic.getOrDefault(topic, 0L);
            long totalCount = attemptsByTopic.get(topic);
            double topicRate = (totalCount == 0) ? 0.0 : (double) correctCount / totalCount;
            successRateByTopic.put(topic, topicRate);
        }

        UserDashboardResponse resp = new UserDashboardResponse();
        resp.setUserId(user.getId());
        resp.setTotalAttempts(totalAttempts);
        resp.setCorrectAttempts(correctAttempts);
        resp.setSuccessRate(successRate);
        resp.setAttemptsByTopic(attemptsByTopic);
        resp.setSuccessRateByTopic(successRateByTopic);

        return resp;
    }

    public AdminDashboardResponse buildAdminDashboard() {
        long totalUsers = userRepository.count();
        List<UserQuestionHistory> allAttempts = historyRepository.findAll();
        long totalAttempts = allAttempts.size();
        long correct = allAttempts.stream().filter(UserQuestionHistory::isCorrect).count();
        double overallRate = (totalAttempts == 0) ? 0.0 : (double) correct / totalAttempts;

        Map<String, Long> attemptsByTopic = new HashMap<>();
        Map<String, Long> correctByTopic = new HashMap<>();

        for (UserQuestionHistory h : allAttempts) {
            String topicName = (h.getQuestion().getTopic() != null)
                    ? h.getQuestion().getTopic().getName()
                    : "Unknown";
            attemptsByTopic.put(topicName, attemptsByTopic.getOrDefault(topicName, 0L) + 1);
            if (h.isCorrect()) {
                correctByTopic.put(topicName, correctByTopic.getOrDefault(topicName, 0L) + 1);
            }
        }

        Map<String, Double> successRateByTopic = new HashMap<>();
        for (String topic : attemptsByTopic.keySet()) {
            long cCount = correctByTopic.getOrDefault(topic, 0L);
            long tCount = attemptsByTopic.get(topic);
            double rate = (tCount == 0) ? 0.0 : (double) cCount / tCount;
            successRateByTopic.put(topic, rate);

            if (rate < 0.1 && tCount > 10) {
                notificationService.notifyAdminErrorPattern(
                        "Very low success rate (" + (rate*100) + "%) detected in topic: " + topic
                );
            }
        }

        AdminDashboardResponse adminResp = new AdminDashboardResponse();
        adminResp.setTotalUsers(totalUsers);
        adminResp.setTotalAttempts(totalAttempts);
        adminResp.setOverallSuccessRate(overallRate);
        adminResp.setAttemptsByTopic(attemptsByTopic);
        adminResp.setSuccessRateByTopic(successRateByTopic);

        return adminResp;
    }
}
