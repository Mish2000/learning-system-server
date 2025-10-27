package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.responses.AdminDashboardResponse;
import com.learningsystemserver.dtos.responses.UserDashboardResponse;
import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseDashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private static final Map<Long, SseEmitter> userEmitters  = new ConcurrentHashMap<>();
    private static final Map<Long, SseEmitter> adminEmitters = new ConcurrentHashMap<>();

    // Single executor for all SSE pushes to avoid doing I/O on controller threads.
    private static final ExecutorService SSE_PUSH_EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sse-push");
        t.setDaemon(true);
        return t;
    });

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
        registerLifecycle(userEmitters, userId, emitter);

        // Send initial snapshot asynchronously
        SSE_PUSH_EXEC.execute(() -> {
            try {
                UserDashboardResponse data = dashboardService.buildUserDashboard(username);
                emitter.send(SseEmitter.event().name("userDashboard").data(data));
            } catch (Exception e) {
                handleEmitterFailure(userEmitters, userId, emitter, "userDashboard", e);
            }
        });

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
        if (user.getRole() == null || user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long userId = user.getId();

        SseEmitter emitter = new SseEmitter(0L);
        registerLifecycle(adminEmitters, userId, emitter);

        // Send initial snapshot asynchronously
        SSE_PUSH_EXEC.execute(() -> {
            try {
                AdminDashboardResponse data = dashboardService.buildAdminDashboard();
                emitter.send(SseEmitter.event().name("adminDashboard").data(data));
            } catch (Exception e) {
                handleEmitterFailure(adminEmitters, userId, emitter, "adminDashboard", e);
            }
        });

        return emitter;
    }

    /** Called by services (e.g., UserHistoryService) to push an updated dashboard to the user. */
    public static void pushUserDash(Long userId, UserDashboardResponse data) {
        safeSend(userEmitters, userId, "dashboard", data);
    }

    /** Called by services to push updated admin dashboard (for ADMIN users). */
    public static void pushAdminDash(Long userId, AdminDashboardResponse data) {
        safeSend(adminEmitters, userId, "adminDashboard", data);
    }

    // ---- internals ----

    private static void registerLifecycle(Map<Long, SseEmitter> map, Long userId, SseEmitter emitter) {
        map.put(userId, emitter);
        emitter.onCompletion(() -> map.remove(userId));
        emitter.onTimeout(() -> {
            try { emitter.complete(); } catch (Exception ignore) {}
            map.remove(userId);
        });
        emitter.onError(ex -> {
            // Treat client aborts as normal disconnects, remove so we won't try again.
            handleEmitterFailure(map, userId, emitter, "lifecycle", ex);
        });
    }

    private static void safeSend(Map<Long, SseEmitter> map, Long userId, String eventName, Object payload) {
        final SseEmitter emitter = map.get(userId);
        if (emitter == null) return;

        // Do not send on the calling (controller/transaction) thread.
        SSE_PUSH_EXEC.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                handleEmitterFailure(map, userId, emitter, eventName, e);
            }
        });
    }

    private static void handleEmitterFailure(Map<Long, SseEmitter> map,
                                             Long userId,
                                             SseEmitter emitter,
                                             String eventName,
                                             Throwable e) {
        if (isClientAbort(e)) {
            // Quiet for expected disconnects
            log.debug("SSE disconnected (event={}): userId={}, cause={}", eventName, userId, rootMessage(e));
        } else {
            // Unexpected write problem
            log.warn("SSE push failed (event={}): userId={}, cause={}", eventName, userId, rootMessage(e));
        }
        try { emitter.complete(); } catch (Exception ignore) {}
        map.remove(userId);
    }

    private static boolean isClientAbort(Throwable e) {
        // unwrap
        Throwable t = e;
        while (t != null) {
            if (t instanceof ClientAbortException) return true;
            if (t instanceof AsyncRequestNotUsableException) return true;
            if (t instanceof IOException) {
                String msg = t.getMessage();
                if (msg != null) {
                    String m = msg.toLowerCase();
                    if (m.contains("broken pipe") ||
                            m.contains("connection reset") ||
                            m.contains("aborted by the software") ||
                            m.contains("async not usable") ||
                            m.contains("pipe is being closed")) {
                        return true;
                    }
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        Throwable last = e;
        while (t != null) {
            last = t;
            t = t.getCause();
        }
        assert last != null;
        return Objects.toString(last.getMessage(), last.getClass().getSimpleName());
    }
}
