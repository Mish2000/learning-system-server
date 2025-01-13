
package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.AdminDashboardResponse;
import com.learningsystemserver.dtos.UserDashboardResponse;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.DashboardService;
import com.learningsystemserver.services.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/sse")
public class SseDashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private static final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();
    private static final Map<Long, SseEmitter> adminEmitters = new ConcurrentHashMap<>();

    public SseDashboardController(DashboardService ds,
                                  UserRepository ur,
                                  JwtService js) {
        this.dashboardService = ds;
        this.userRepository = ur;
        this.jwtService = js;
    }

    @GetMapping(value = "/user-dashboard", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectUser(@RequestParam("token") String token) {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = authenticate(token);
        userEmitters.put(userId, emitter);

        emitter.onCompletion(() -> userEmitters.remove(userId));
        emitter.onTimeout(() -> userEmitters.remove(userId));

        new Thread(() -> {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    UserDashboardResponse data = dashboardService.buildUserDashboard(user.getUsername());
                    emitter.send(SseEmitter.event().name("userDashboard").data(data));
                }
            } catch (IOException e) {
                userEmitters.remove(userId);
            }
        }).start();

        return emitter;
    }

    @GetMapping(value = "/admin-dashboard", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectAdmin(@RequestParam("token") String token) {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = authenticateAdmin(token);
        adminEmitters.put(userId, emitter);

        emitter.onCompletion(() -> adminEmitters.remove(userId));
        emitter.onTimeout(() -> adminEmitters.remove(userId));

        new Thread(() -> {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    AdminDashboardResponse data = dashboardService.buildAdminDashboard();
                    emitter.send(SseEmitter.event().name("adminDashboard").data(data));
                }
            } catch (IOException e) {
                userEmitters.remove(userId);
            }
        }).start();

        return emitter;
    }

    public static void pushUserDash(Long userId, UserDashboardResponse data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("userDashboard").data(data));
            } catch (IOException e) {
                userEmitters.remove(userId);
                log.info("SSE user dashboard push failed (client disconnected?) userId={}, error={}", userId, e.getMessage());
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

    private Long authenticate(String token) {
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("No user found"));
        return user.getId();
    }

    private Long authenticateAdmin(String token) {
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("No user found"));
        if (!user.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Not admin");
        }
        return user.getId();
    }
}

