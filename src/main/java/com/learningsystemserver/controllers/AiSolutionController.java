package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.StreamingOllamaService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class AiSolutionController {

    private final StreamingOllamaService blockingService;
    private final UserRepository userRepository;

    public AiSolutionController(StreamingOllamaService svc, UserRepository userRepository) {
        this.blockingService = svc;
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAiSolution(
            @RequestParam("question") String questionText,
            @RequestParam(value = "lang", required = false) String lang,
            Authentication authentication
    ) {
        String effectiveLang = normalizeLang(lang);

        if (effectiveLang == null && authentication != null) {
            String username = authentication.getName();
            User u = userRepository.findByUsername(username).orElse(null);
            if (u != null) {
                effectiveLang = normalizeLang(u.getInterfaceLanguage());
            }
        }
        if (effectiveLang == null) {
            effectiveLang = "he";
        }

        return blockingService.streamSolution(questionText, effectiveLang);
    }

    private String normalizeLang(String v) {
        if (v == null) return null;
        v = v.trim().toLowerCase();
        if (v.startsWith("he")) return "he";
        if (v.startsWith("en")) return "en";
        return null;
    }
}
