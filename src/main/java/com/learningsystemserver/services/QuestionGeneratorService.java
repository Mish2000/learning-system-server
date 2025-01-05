package com.learningsystemserver.services;

import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.GeneratedQuestion;
import com.learningsystemserver.entities.Topic;
import com.learningsystemserver.repositories.GeneratedQuestionRepository;
import com.learningsystemserver.repositories.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class QuestionGeneratorService {

    private final TopicRepository topicRepository;
    private final GeneratedQuestionRepository questionRepository;

    private final Random random = new Random();

    public GeneratedQuestion generateQuestion(Long topicId, DifficultyLevel difficultyLevel) {
        Topic topic = null;
        if (topicId != null) {
            Optional<Topic> opt = topicRepository.findById(topicId);
            if (opt.isPresent()) {
                topic = opt.get();
            }
        }

        if (topic != null && topic.getName().toLowerCase().contains("addition")) {
            return createAdditionQuestion(topic, difficultyLevel);
        } else {
            return createAdditionQuestion(topic, difficultyLevel);
        }
    }

    private GeneratedQuestion createAdditionQuestion(Topic topic, DifficultyLevel difficulty) {
        int rangeMin = 1;
        int rangeMax = 10;

        switch (difficulty) {
            case EASY -> {
                rangeMax = 30;
            }
            case MEDIUM -> {
                rangeMax = 100;
            }
            case ADVANCED -> {
                rangeMax = 1000;
            }
            default -> {

            }
        }

        int a = random.nextInt(rangeMax - rangeMin + 1) + rangeMin;
        int b = random.nextInt(rangeMax - rangeMin + 1) + rangeMin;
        int answer = a + b;

        String questionText = a + " + " + b + " = ?";
        String solutionSteps = "Step 1: Take " + a + " and add " + b + ".\n"
                + "Step 2: The result is " + answer + ".";

        GeneratedQuestion generated = GeneratedQuestion.builder()
                .questionText(questionText)
                .solutionSteps(solutionSteps)
                .correctAnswer(String.valueOf(answer))
                .topic(topic)
                .difficultyLevel(difficulty)
                .build();

        return questionRepository.save(generated);
    }

    public GeneratedQuestion getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
    }
}
