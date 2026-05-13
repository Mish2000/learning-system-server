package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Notification;
import com.learningsystemserver.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    public List<Notification> getAllNotifications(String username) {
        return notificationRepository.findByRecipientUsername(username);
    }

    public void markNotificationAsRead(Long id, String username) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getRecipientUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public Notification createNotification(String message, String recipientUsername, String type) {
        Notification notification = Notification.builder()
                .message(message)
                .recipientUsername(recipientUsername)
                .type(type)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        sseEmitterService.sendNotification(recipientUsername, saved);
        return saved;
    }

    public void notifyUserDifficulty(String username, String topicName) {
        String message = String.format(
                "You are having difficulty with %s. We recommend you practice more!",
                topicName
        );
        createNotification(message, username, "USER_WARNING");
    }

    public void notifyUserSuccess(String username, String topicName) {
        String message = String.format(
                "You're doing great at %s! Keep up the good work!",
                topicName
        );
        createNotification(message, username, "USER_SUCCESS");
    }

    public void notifyAdminErrorPattern(String patternDescription) {
        String adminUsername = "admin";
        String message = "A new error pattern was detected: " + patternDescription;
        createNotification(message, adminUsername, "ADMIN_ALERT");
    }

    public void clearAllNotificationsForUser(String username) {
        notificationRepository.deleteByRecipientUsername(username);
    }
}




