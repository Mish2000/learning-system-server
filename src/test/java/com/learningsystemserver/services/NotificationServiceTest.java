package com.learningsystemserver.services;

import com.learningsystemserver.entities.Notification;
import com.learningsystemserver.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SseEmitterService sseEmitterService;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, sseEmitterService);
    }

    @Test
    void markNotificationAsReadMarksOwnNotification() {
        Notification notification = notificationFor("alice");
        when(notificationRepository.findById(42L)).thenReturn(Optional.of(notification));

        service.markNotificationAsRead(42L, "alice");

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markNotificationAsReadRejectsOtherUsersNotification() {
        Notification notification = notificationFor("bob");
        when(notificationRepository.findById(42L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markNotificationAsRead(42L, "alice"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("Forbidden");
                });

        assertThat(notification.isRead()).isFalse();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markNotificationAsReadReturnsNotFoundWhenNotificationDoesNotExist() {
        when(notificationRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markNotificationAsRead(42L, "alice"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
                );

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    private static Notification notificationFor(String recipientUsername) {
        return Notification.builder()
                .id(42L)
                .message("message")
                .recipientUsername(recipientUsername)
                .type("USER_SUCCESS")
                .isRead(false)
                .build();
    }
}
