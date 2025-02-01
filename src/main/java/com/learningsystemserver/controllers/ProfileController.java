package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.dtos.UpdateProfileRequest;
import com.learningsystemserver.dtos.ProfileResponse;
import com.learningsystemserver.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

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
        String base64Image = null;
        if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
            base64Image = Base64.getEncoder().encodeToString(user.getProfileImage());
        }
        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .profileImage(base64Image)
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
        String base64Image = null;
        if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
            base64Image = Base64.getEncoder().encodeToString(user.getProfileImage());
        }
        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .profileImage(base64Image)
                .build();
    }

    @PostMapping("/uploadImage")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new RuntimeException("No user with username: " + principalName));
        try {
            user.setProfileImage(image.getBytes());
            userRepository.save(user);
            return ResponseEntity.ok("Image uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image");
        }
    }
}

