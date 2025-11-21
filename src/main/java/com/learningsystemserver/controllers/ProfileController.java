package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.dtos.requests.UpdateProfileRequest;
import com.learningsystemserver.dtos.responses.ProfileResponse;
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
    public ProfileResponse getProfile() throws InvalidInputException {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException("No user with username: " + principalName));

        String base64Image = null;
        if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
            base64Image = Base64.getEncoder().encodeToString(user.getProfileImage());
        }

        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .profileImage(base64Image)
                .subDifficultyLevel(user.getSubDifficultyLevel())
                .currentDifficulty(
                        user.getOverallProgressLevel() != null ? user.getOverallProgressLevel().name() : "BASIC"
                )
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }


    @PutMapping
    public ProfileResponse updateProfile(@RequestBody UpdateProfileRequest request)
            throws AlreadyInUseException, InvalidInputException {

        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException("No user with username: " + principalName));

        boolean usernameChanged = false;
        if (request.getUsername() != null && !request.getUsername().isEmpty() &&
                !request.getUsername().equals(user.getUsername())) {

            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AlreadyInUseException("Username already in use.");
            }
            user.setUsername(request.getUsername());
            usernameChanged = true;
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getInterfaceLanguage() != null && !request.getInterfaceLanguage().isBlank()) {
            user.setInterfaceLanguage(com.learningsystemserver.utils.LanguageUtils.normalize(request.getInterfaceLanguage()));
        }

        userRepository.save(user);

        String base64Image = null;
        if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
            base64Image = Base64.getEncoder().encodeToString(user.getProfileImage());
        }

        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .interfaceLanguage(user.getInterfaceLanguage())
                .profileImage(base64Image);

        if (usernameChanged) {
            String newToken = jwtService.generateToken(user.getUsername());
            builder.newToken(newToken);
        }

        return builder.build();
    }

    @PostMapping("/uploadImage")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) throws InvalidInputException {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException("No user with username: " + principalName));
        try {
            user.setProfileImage(image.getBytes());
            userRepository.save(user);
            return ResponseEntity.ok("Image uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image");
        }
    }

    @DeleteMapping({"/image", "/image/delete"})
    public ResponseEntity<Void> deleteImage() throws InvalidInputException {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException("No user with username: " + principalName));

        user.setProfileImage(null);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/image/delete")
    public ResponseEntity<Void> deleteImageAlias() throws InvalidInputException {
        return deleteImage();
    }

}


