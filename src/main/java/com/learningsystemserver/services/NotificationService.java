package com.learningsystemserver.services;

import com.learningsystemserver.entities.Notification;
import com.learningsystemserver.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public void markNotificationAsRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        n.setRead(true);
        notificationRepository.save(n);
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

    public void notifyAdminErrorPattern(String patternDescription) {
        String adminUsername = "admin";
        String message = "A new error pattern was detected: " + patternDescription;
        createNotification(message, adminUsername, "ADMIN_ALERT");
    }

    public void clearAllNotificationsForUser(String username) {
        notificationRepository.deleteByRecipientUsername(username);
    }
}




