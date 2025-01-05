package com.learningsystemserver.controllers;
/*
import com.learningsystemserver.dtos.UserDashboardResponse;
import com.learningsystemserver.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/user")
@RequiredArgsConstructor
public class UserDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public UserDashboardResponse getUserDashboard() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return dashboardService.buildUserDashboard(email);
    }
}


 */