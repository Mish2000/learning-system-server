package com.learningsystemserver.services;

import com.learningsystemserver.entities.Notification;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseEmitterService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void addEmitter(String username, SseEmitter emitter) {
        emitters.computeIfAbsent(username, k -> new ArrayList<>()).add(emitter);
    }

    public void removeEmitter(String username, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
        }
    }

    public void sendNotification(String username, Notification notification) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(notification));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            userEmitters.removeAll(deadEmitters);
        }
    }
}
