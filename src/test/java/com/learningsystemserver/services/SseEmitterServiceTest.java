package com.learningsystemserver.services;

import com.learningsystemserver.entities.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    private SseEmitterService service;

    @Mock
    private SseEmitter firstEmitter;
    @Mock
    private SseEmitter secondEmitter;

    @BeforeEach
    void setUp() {
        service = new SseEmitterService();
    }

    @Test
    void sendNotificationSendsToMultipleEmittersForSameUser() throws IOException {
        service.addEmitter("alice", firstEmitter);
        service.addEmitter("alice", secondEmitter);

        service.sendNotification("alice", notification());

        verify(firstEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(secondEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void addEmitterRegistersLifecycleCallbacks() {
        service.addEmitter("alice", firstEmitter);

        verify(firstEmitter).onCompletion(any(Runnable.class));
        verify(firstEmitter).onTimeout(any(Runnable.class));
        verify(firstEmitter).onError(any());
    }

    @Test
    void completionRemovesEmitterAndCleansEmptyUserEntry() throws Exception {
        ArgumentCaptor<Runnable> completionCaptor = ArgumentCaptor.forClass(Runnable.class);
        service.addEmitter("alice", firstEmitter);
        verify(firstEmitter).onCompletion(completionCaptor.capture());

        completionCaptor.getValue().run();
        service.sendNotification("alice", notification());

        verify(firstEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(emitters()).doesNotContainKey("alice");
    }

    @Test
    void sendNotificationRemovesDeadEmittersWithoutSkippingLiveEmitters() throws IOException {
        doThrow(new IOException("client closed"))
                .when(firstEmitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        service.addEmitter("alice", firstEmitter);
        service.addEmitter("alice", secondEmitter);

        service.sendNotification("alice", notification());
        service.sendNotification("alice", notification());

        verify(firstEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(secondEmitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void sendNotificationRemovesCompletedEmitters() throws Exception {
        doThrow(new IllegalStateException("already completed"))
                .when(firstEmitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        service.addEmitter("alice", firstEmitter);

        service.sendNotification("alice", notification());
        service.sendNotification("alice", notification());

        verify(firstEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(emitters()).doesNotContainKey("alice");
    }

    private static Notification notification() {
        return Notification.builder()
                .message("message")
                .recipientUsername("alice")
                .type("USER_SUCCESS")
                .isRead(false)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> emitters() throws NoSuchFieldException, IllegalAccessException {
        Field field = SseEmitterService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(service);
    }
}
