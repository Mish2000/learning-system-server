package com.learningsystemserver.services;

import com.learningsystemserver.controllers.SseDashboardController;
import com.learningsystemserver.dtos.AdminDashboardResponse;
import com.learningsystemserver.dtos.UserDashboardResponse;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.entities.UserQuestionHistory;
import com.learningsystemserver.repositories.GeneratedQuestionRepository;
import com.learningsystemserver.repositories.UserQuestionHistoryRepository;
import com.learningsystemserver.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private final UserQuestionHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final GeneratedQuestionRepository questionRepository;
    private final AdaptiveService adaptiveService;
    private final DashboardService dashboardService;

    @Transactional
    public void logAttempt(Long userId,
                           Long questionId,
                           boolean correct,
                           String userAnswer,
                           Long timeTakenSeconds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        GeneratedQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));

        UserQuestionHistory history = UserQuestionHistory.builder()
                .user(user)
                .question(question)
                .correct(correct)
                .userAnswer(userAnswer)
                .attemptTime(LocalDateTime.now())
                .timeTakenSeconds(timeTakenSeconds)
                .build();
        historyRepository.save(history);

        adaptiveService.evaluateUserProgress(userId);

        UserDashboardResponse userData = dashboardService.buildUserDashboard(user.getEmail());
        SseDashboardController.pushUserDash(user.getId(), userData);

        if (user.getRole() == Role.ADMIN) {
            AdminDashboardResponse adminData = dashboardService.buildAdminDashboard();
            SseDashboardController.pushAdminDash(user.getId(), adminData);
        }
    }

}

