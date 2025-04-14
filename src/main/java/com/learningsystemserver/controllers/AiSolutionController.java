package com.learningsystemserver.controllers;

import com.learningsystemserver.services.StreamingOllamaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class AiSolutionController {

    private final StreamingOllamaService blockingService;

    public AiSolutionController(StreamingOllamaService svc) {
        this.blockingService = svc;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAiSolution(@RequestParam("question") String questionText) {
        return blockingService.streamSolution(questionText);
    }
}

