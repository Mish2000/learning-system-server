package com.learningsystemserver.dtos.requests;

import com.learningsystemserver.entities.DifficultyLevel;
import lombok.Data;

@Data
public class QuestionRequest {
    private Long topicId;
    private DifficultyLevel difficultyLevel;
}
