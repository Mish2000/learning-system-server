package com.learningsystemserver.controllers;

import com.learningsystemserver.services.NotificationService;
import com.learningsystemserver.services.SseEmitterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private SseEmitterService sseEmitterService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService, sseEmitterService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markAsReadUsesAuthenticatedUsername() {
        ResponseEntity<String> response = controller.markAsRead(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).markNotificationAsRead(42L, "alice");
    }
}
