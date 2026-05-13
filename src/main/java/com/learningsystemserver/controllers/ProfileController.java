package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.dtos.requests.UpdateProfileRequest;
import com.learningsystemserver.dtos.responses.ProfileResponse;
import com.learningsystemserver.services.JwtService;
import com.learningsystemserver.utils.CookieUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private static final long MAX_PROFILE_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_PROFILE_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    @Value("${security.cookies.secure:false}")
    private boolean secureCookies;

    @GetMapping
    public ProfileResponse getProfile() throws InvalidInputException {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException("No user with username: " + principalName));

        return buildProfileResponse(user);
    }


    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request)
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

        if (usernameChanged) {
            var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String access = jwtService.generateAccessToken(userDetails);
            String refresh = jwtService.generateRefreshToken(userDetails);

            var headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, CookieUtils.accessCookie(access, secureCookies).toString());
            headers.add(HttpHeaders.SET_COOKIE, CookieUtils.refreshCookie(refresh, secureCookies).toString());

            return ResponseEntity.ok().headers(headers).body(buildProfileResponse(user));
        }

        return ResponseEntity.ok(buildProfileResponse(user));
    }

    @PostMapping("/uploadImage")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) throws InvalidInputException {
        ResponseEntity<String> metadataValidationError = validateProfileImageMetadata(image);
        if (metadataValidationError != null) {
            return metadataValidationError;
        }

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image");
        }

        ResponseEntity<String> contentValidationError = validateProfileImageContent(
                imageBytes,
                normalizeContentType(image.getContentType())
        );
        if (contentValidationError != null) {
            return contentValidationError;
        }

        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(principalName)
                .orElseThrow(() -> new InvalidInputException("No user with username: " + principalName));

        user.setProfileImage(imageBytes);
        userRepository.save(user);
        return ResponseEntity.ok("Profile image uploaded successfully.");
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

    private ProfileResponse buildProfileResponse(User user) {
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

    private ResponseEntity<String> validateProfileImageMetadata(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("Profile image file is required.");
        }
        if (image.getSize() > MAX_PROFILE_IMAGE_BYTES) {
            return ResponseEntity.badRequest().body("Profile image must be 2MB or smaller.");
        }

        String contentType = normalizeContentType(image.getContentType());
        if (!ALLOWED_PROFILE_IMAGE_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body("Profile image must be a JPEG, PNG, or WebP file.");
        }

        return null;
    }

    private ResponseEntity<String> validateProfileImageContent(byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.badRequest().body("Profile image file is required.");
        }
        if (imageBytes.length > MAX_PROFILE_IMAGE_BYTES) {
            return ResponseEntity.badRequest().body("Profile image must be 2MB or smaller.");
        }
        if (!matchesDeclaredImageType(imageBytes, contentType)) {
            return ResponseEntity.badRequest().body("Profile image content does not match a supported image format.");
        }

        return null;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesDeclaredImageType(byte[] imageBytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> isJpeg(imageBytes);
            case "image/png" -> isPng(imageBytes);
            case "image/webp" -> isWebp(imageBytes);
            default -> false;
        };
    }

    private boolean isJpeg(byte[] imageBytes) {
        return imageBytes.length >= 3
                && imageBytes[0] == (byte) 0xFF
                && imageBytes[1] == (byte) 0xD8
                && imageBytes[2] == (byte) 0xFF;
    }

    private boolean isPng(byte[] imageBytes) {
        return imageBytes.length >= 8
                && imageBytes[0] == (byte) 0x89
                && imageBytes[1] == 'P'
                && imageBytes[2] == 'N'
                && imageBytes[3] == 'G'
                && imageBytes[4] == 0x0D
                && imageBytes[5] == 0x0A
                && imageBytes[6] == 0x1A
                && imageBytes[7] == 0x0A;
    }

    private boolean isWebp(byte[] imageBytes) {
        return imageBytes.length >= 12
                && imageBytes[0] == 'R'
                && imageBytes[1] == 'I'
                && imageBytes[2] == 'F'
                && imageBytes[3] == 'F'
                && imageBytes[8] == 'W'
                && imageBytes[9] == 'E'
                && imageBytes[10] == 'B'
                && imageBytes[11] == 'P';
    }

}


