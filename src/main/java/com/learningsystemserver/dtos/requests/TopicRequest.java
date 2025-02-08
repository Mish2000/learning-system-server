package com.learningsystemserver.dtos.requests;

import com.learningsystemserver.entities.DifficultyLevel;
import lombok.Data;

@Data
public class TopicRequest {
    private String name;
    private String description;
    private DifficultyLevel difficultyLevel;
    private Long parentId;
}
