package com.learningsystemserver.services;

import com.learningsystemserver.entities.Notification;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEmitterService {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void addEmitter(String username, SseEmitter emitter) {
        emitter.onCompletion(() -> removeEmitter(username, emitter));
        emitter.onTimeout(() -> removeEmitter(username, emitter));
        emitter.onError(error -> removeEmitter(username, emitter));

        emitters.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void removeEmitter(String username, SseEmitter emitter) {
        emitters.computeIfPresent(username, (key, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }

    public void sendNotification(String username, Notification notification) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(notification));
                } catch (IOException | IllegalStateException e) {
                    removeEmitter(username, emitter);
                }
            }
        }
    }
}
