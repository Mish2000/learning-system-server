package com.learningsystemserver.dtos;

import com.learningsystemserver.entities.DifficultyLevel;
import lombok.Data;

@Data
public class QuestionRequest {
    private Long topicId;
    private DifficultyLevel difficultyLevel;
}
