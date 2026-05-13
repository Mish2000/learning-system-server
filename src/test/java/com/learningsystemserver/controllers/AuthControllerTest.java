package com.learningsystemserver.controllers;

import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.JwtService;
import com.learningsystemserver.services.UserProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserProgressService userProgressService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authenticationManager,
                userDetailsService,
                jwtService,
                userRepository,
                passwordEncoder,
                userProgressService
        );
    }

    @Test
    void loginForNormalUserReturnsUserAuthorityAndDoesNotMutateRole() {
        User user = userWithRole(Role.USER);
        AuthController.LoginRequest request = loginRequest();
        UserDetails userDetails = userDetailsWithAuthority("ROLE_USER");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("student")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh");

        ResponseEntity<?> response = controller.login(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((AuthController.LoginResponse) response.getBody()).getRole()).isEqualTo("ROLE_USER");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).save(user);
    }

    @Test
    void loginForAdminUserReturnsAdminAuthorityAndDoesNotMutateRole() {
        User user = userWithRole(Role.ADMIN);
        AuthController.LoginRequest request = loginRequest();
        UserDetails userDetails = userDetailsWithAuthority("ROLE_ADMIN");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("student")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh");

        ResponseEntity<?> response = controller.login(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((AuthController.LoginResponse) response.getBody()).getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(user);
    }

    @Test
    void loginRequestDoesNotExposeAdminPrivilegeField() {
        assertThat(Arrays.stream(AuthController.LoginRequest.class.getDeclaredFields())
                .map(Field::getName))
                .doesNotContain("isAdmin");
        assertThat(Arrays.stream(AuthController.LoginRequest.class.getMethods())
                .map(Method::getName))
                .doesNotContain("isAdmin", "setAdmin", "setIsAdmin");
    }

    private static AuthController.LoginRequest loginRequest() {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("password");
        return request;
    }

    private static User userWithRole(Role role) {
        User user = new User();
        user.setUsername("student");
        user.setEmail("student@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setInterfaceLanguage("en");
        return user;
    }

    private static UserDetails userDetailsWithAuthority(String authority) {
        return org.springframework.security.core.userdetails.User.withUsername("student")
                .password("encoded-password")
                .authorities(authority)
                .build();
    }
}
