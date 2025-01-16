package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.requests.UpdateProfileRequest;
import com.learningsystemserver.dtos.ProfileResponse;
import com.learningsystemserver.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping
    public ProfileResponse getProfile() {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new RuntimeException("No user with username: " + principalName));

        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .solutionDetailLevel(user.getSolutionDetailLevel())
                .build();
    }

    @PutMapping
    public ProfileResponse updateProfile(@RequestBody UpdateProfileRequest request) {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new RuntimeException("No user with username: " + principalName));

        if (request.getInterfaceLanguage() != null) {
            user.setInterfaceLanguage(request.getInterfaceLanguage());
        }
        if (request.getSolutionDetailLevel() != null) {
            user.setSolutionDetailLevel(request.getSolutionDetailLevel());
        }

        userRepository.save(user);

        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .solutionDetailLevel(user.getSolutionDetailLevel())
                .build();
    }
}
