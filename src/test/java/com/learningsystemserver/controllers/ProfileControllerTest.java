package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.requests.UpdateProfileRequest;
import com.learningsystemserver.dtos.responses.ProfileResponse;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDetailsService userDetailsService;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(userRepository, jwtService, passwordEncoder, userDetailsService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("old-user", null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateProfileChangingUsernameSetsFreshAuthCookiesAndDoesNotExposeToken() throws Exception {
        User user = user("old-user");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("new-user");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("new-user")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();

        when(userRepository.findByUsername("old-user")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("new-user")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("fresh-access");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("fresh-refresh");

        ResponseEntity<ProfileResponse> response = controller.updateProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("new-user");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .hasSize(2)
                .anySatisfy(cookie -> assertThat(cookie).startsWith("access_token=fresh-access"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token=fresh-refresh"));
        assertThat(Arrays.stream(ProfileResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("newToken");
    }

    @Test
    void updateProfileWithoutUsernameChangeDoesNotIssueAuthCookies() throws Exception {
        User user = user("old-user");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("old-user");
        request.setPassword("New-password1!");
        request.setInterfaceLanguage("he");

        when(userRepository.findByUsername("old-user")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("New-password1!")).thenReturn("encoded-new-password");

        ResponseEntity<ProfileResponse> response = controller.updateProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.SET_COOKIE);
        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getInterfaceLanguage()).isEqualTo("he");
        verify(userDetailsService, never()).loadUserByUsername("old-user");
        verify(jwtService, never()).generateAccessToken(org.mockito.ArgumentMatchers.any());
        verify(jwtService, never()).generateRefreshToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void uploadImageStoresValidPngProfileImage() throws Exception {
        User user = user("old-user");
        byte[] imageBytes = pngBytes();
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", imageBytes);

        when(userRepository.findByUsername("old-user")).thenReturn(Optional.of(user));

        ResponseEntity<String> response = controller.uploadImage(image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Profile image uploaded successfully.");
        assertThat(user.getProfileImage()).isEqualTo(imageBytes);
        verify(userRepository).save(user);
    }

    @Test
    void uploadImageStoresValidWebpProfileImage() throws Exception {
        User user = user("old-user");
        byte[] imageBytes = webpBytes();
        MockMultipartFile image = new MockMultipartFile("image", "avatar.webp", "image/webp", imageBytes);

        when(userRepository.findByUsername("old-user")).thenReturn(Optional.of(user));

        ResponseEntity<String> response = controller.uploadImage(image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(user.getProfileImage()).isEqualTo(imageBytes);
        verify(userRepository).save(user);
    }

    @Test
    void uploadImageRejectsEmptyFile() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

        ResponseEntity<String> response = controller.uploadImage(image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Profile image file is required.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadImageRejectsOversizedFile() throws Exception {
        byte[] oversizedImage = new byte[(2 * 1024 * 1024) + 1];
        byte[] pngHeader = pngBytes();
        System.arraycopy(pngHeader, 0, oversizedImage, 0, pngHeader.length);
        MockMultipartFile image = new MockMultipartFile("image", "large.png", "image/png", oversizedImage);

        ResponseEntity<String> response = controller.uploadImage(image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Profile image must be 2MB or smaller.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadImageRejectsUnsupportedContentType() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "notes.txt", "text/plain", "hello".getBytes());

        ResponseEntity<String> response = controller.uploadImage(image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Profile image must be a JPEG, PNG, or WebP file.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadImageRejectsDisguisedNonImageContent() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "fake.png",
                "image/png",
                "not a real image".getBytes()
        );

        ResponseEntity<String> response = controller.uploadImage(image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Profile image content does not match a supported image format.");
        verify(userRepository, never()).save(any());
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setInterfaceLanguage("en");
        user.setSubDifficultyLevel(0);
        user.setOverallProgressLevel(DifficultyLevel.BASIC);
        user.setOverallProgressScore(1.0);
        return user;
    }

    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 'P', 'N', 'G',
                0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0
        };
    }

    private static byte[] webpBytes() {
        return new byte[]{
                'R', 'I', 'F', 'F',
                0, 0, 0, 0,
                'W', 'E', 'B', 'P',
                0, 0, 0, 0
        };
    }
}
