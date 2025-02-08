package com.learningsystemserver.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitAnswerResponse {
    private boolean correct;
    private String correctAnswer;
    private String solutionSteps;
}
