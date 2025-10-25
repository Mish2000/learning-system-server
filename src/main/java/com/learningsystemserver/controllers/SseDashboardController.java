package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.responses.AdminDashboardResponse;
import com.learningsystemserver.dtos.responses.UserDashboardResponse;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseDashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private static final Map<Long, SseEmitter> userEmitters  = new ConcurrentHashMap<>();
    private static final Map<Long, SseEmitter> adminEmitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/user-dashboard", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectUser(Authentication auth) throws InvalidInputException {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidInputException("No user found for " + username));
        Long userId = user.getId();

        SseEmitter emitter = new SseEmitter(0L);
        userEmitters.put(userId, emitter);

        emitter.onCompletion(() -> userEmitters.remove(userId));
        emitter.onTimeout(() -> userEmitters.remove(userId));

        new Thread(() -> {
            try {
                UserDashboardResponse data = dashboardService.buildUserDashboard(username);
                emitter.send(SseEmitter.event().name("userDashboard").data(data));
            } catch (Exception e) {
                userEmitters.remove(userId);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        }).start();

        return emitter;
    }

    @GetMapping(value = "/admin-dashboard", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() == null || !"ADMIN".equals(user.getRole().name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long userId = user.getId();

        SseEmitter emitter = new SseEmitter(0L);
        adminEmitters.put(userId, emitter);

        emitter.onCompletion(() -> adminEmitters.remove(userId));
        emitter.onTimeout(() -> adminEmitters.remove(userId));

        new Thread(() -> {
            try {
                AdminDashboardResponse data = dashboardService.buildAdminDashboard();
                emitter.send(SseEmitter.event().name("adminDashboard").data(data));
            } catch (Exception e) {
                adminEmitters.remove(userId);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        }).start();

        return emitter;
    }

    // === unchanged push helpers ===
    public static void pushUserDash(Long userId, UserDashboardResponse data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("userDashboard").data(data));
            } catch (IOException e) {
                userEmitters.remove(userId);
                log.info("SSE user dashboard push failed, userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    public static void pushAdminDash(Long userId, AdminDashboardResponse data) {
        SseEmitter emitter = adminEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("adminDashboard").data(data));
            } catch (IOException e) {
                adminEmitters.remove(userId);
            }
        }
    }
}
