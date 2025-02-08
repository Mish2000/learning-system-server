package com.learningsystemserver.dtos.responses;

import lombok.Data;

import java.util.Map;

@Data
public class AdminDashboardResponse {

    private long totalUsers;
    private long totalAttempts;
    private double overallSuccessRate;

    private Map<String, Long> attemptsByTopic;

    private Map<String, Double> successRateByTopic;
}
