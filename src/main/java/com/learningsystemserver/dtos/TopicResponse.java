package com.learningsystemserver.dtos;

import lombok.Data;

@Data
public class TopicResponse {
    private Long id;
    private String name;
    private String description;
    private String difficultyLevel;
    private Long parentId; // or null if top-level
}
