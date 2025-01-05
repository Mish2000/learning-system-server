package com.learningsystemserver.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String questionText;
    private String solutionSteps;
    private String correctAnswer;
    private Long topicId;
    private String difficultyLevel;
}
