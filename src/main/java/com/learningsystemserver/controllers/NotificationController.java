package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.Notification;
import com.learningsystemserver.services.NotificationService;
import com.learningsystemserver.services.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping("/stream")
    public SseEmitter streamNotifications() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SseEmitter emitter = new SseEmitter(60000L);
        sseEmitterService.addEmitter(username, emitter);
        emitter.onCompletion(() -> sseEmitterService.removeEmitter(username, emitter));
        emitter.onTimeout(() -> sseEmitterService.removeEmitter(username, emitter));
        return emitter;
    }

    @GetMapping
    public List<Notification> getNotifications() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return notificationService.getAllNotifications(username);
    }

    @PostMapping("/markRead/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        notificationService.markNotificationAsRead(id);
        return ResponseEntity.ok("Marked as read");
    }

    @DeleteMapping("/clearAll")
    public ResponseEntity<Void> clearAllForCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.clearAllNotificationsForUser(username);
        return ResponseEntity.noContent().build();
    }

}
