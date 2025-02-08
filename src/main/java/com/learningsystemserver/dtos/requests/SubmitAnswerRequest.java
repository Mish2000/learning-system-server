package com.learningsystemserver.dtos.requests;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long questionId;
    private String userAnswer;
    private Long userId;
}
