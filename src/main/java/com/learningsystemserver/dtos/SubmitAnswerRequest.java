package com.learningsystemserver.dtos;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long questionId;
    private String userAnswer;
    private Long userId;
}
