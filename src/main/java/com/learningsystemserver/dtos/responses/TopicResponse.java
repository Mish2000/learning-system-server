package com.learningsystemserver.dtos.responses;

import lombok.Data;

@Data
public class TopicResponse {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private int subtopicCount;
}
