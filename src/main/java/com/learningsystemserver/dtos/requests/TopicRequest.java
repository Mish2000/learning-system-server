package com.learningsystemserver.dtos.requests;

import lombok.Data;

@Data
public class TopicRequest {
    private String name;
    private String description;
    private Long parentId;
}
