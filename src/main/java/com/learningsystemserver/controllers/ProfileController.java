package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.dtos.UpdateProfileRequest;
import com.learningsystemserver.dtos.ProfileResponse;
import com.learningsystemserver.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ProfileResponse getProfile() {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new RuntimeException("No user with username: " + principalName));

        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .build();
    }

    @PutMapping
    public ProfileResponse updateProfile(@RequestBody UpdateProfileRequest request) {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new RuntimeException("No user with username: " + principalName));

        if (request.getUsername() != null && !request.getUsername().isEmpty() &&
                !request.getUsername().equals(user.getUsername())) {
            if(userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already in use.");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getInterfaceLanguage() != null) {
            user.setInterfaceLanguage(request.getInterfaceLanguage());
        }

        userRepository.save(user);

        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .build();
    }
}

