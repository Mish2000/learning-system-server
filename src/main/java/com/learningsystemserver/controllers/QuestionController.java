package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.QuestionRequest;
import com.learningsystemserver.dtos.QuestionResponse;
import com.learningsystemserver.dtos.SubmitAnswerRequest;
import com.learningsystemserver.dtos.SubmitAnswerResponse;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.QuestionGeneratorService;
import com.learningsystemserver.services.UserHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGeneratorService questionService;
    private final UserHistoryService userHistoryService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    public QuestionResponse generateQuestion(@RequestBody QuestionRequest request) {
        DifficultyLevel level = request.getDifficultyLevel() != null
                ? request.getDifficultyLevel() : DifficultyLevel.BASIC;
        GeneratedQuestion q = questionService.generateQuestion(request.getTopicId(), level);
        return toResponse(q);
    }

    @GetMapping("/{id}")
    public QuestionResponse getQuestion(@PathVariable Long id) {
        GeneratedQuestion q = questionService.getQuestionById(id);
        return toResponse(q);
    }

    @PostMapping("/submit")
    public SubmitAnswerResponse submitAnswer(@RequestBody SubmitAnswerRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user with email: " + email));
        GeneratedQuestion q = questionService.getQuestionById(request.getQuestionId());
        boolean isCorrect = q.getCorrectAnswer().equalsIgnoreCase(request.getUserAnswer());

        userHistoryService.logAttempt(
                user.getId(),
                q.getId(),
                isCorrect,
                request.getUserAnswer(),
                null
        );

        return new SubmitAnswerResponse(isCorrect, q.getCorrectAnswer(), q.getSolutionSteps());
    }

    private QuestionResponse toResponse(GeneratedQuestion q) {
        return new QuestionResponse(
                q.getId(),
                q.getQuestionText(),
                q.getSolutionSteps(),
                q.getCorrectAnswer(),
                (q.getTopic() != null) ? q.getTopic().getId() : null,
                (q.getDifficultyLevel() != null) ? q.getDifficultyLevel().name() : null
        );
    }
}
