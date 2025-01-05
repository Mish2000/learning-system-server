package com.learningsystemserver.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class UserDashboardResponse {

    private Long userId;
    private long totalAttempts;
    private long correctAttempts;
    private double successRate;


    private Map<String, Long> attemptsByTopic;


    private Map<String, Double> successRateByTopic;
}
