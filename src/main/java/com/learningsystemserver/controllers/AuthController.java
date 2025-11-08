package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.requests.RegisterRequest;
import com.learningsystemserver.dtos.responses.AuthResponse;
import com.learningsystemserver.entities.DifficultyLevel;
import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.ErrorMessages;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.JwtService;
import com.learningsystemserver.services.UserProgressService;
import com.learningsystemserver.utils.CookieUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Value("${security.cookies.secure:false}")
    private boolean secureCookies;

    private final UserProgressService userProgressService;


    public AuthController(
            AuthenticationManager am,
            UserDetailsService uds,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder, UserProgressService userProgressService
    ) {
        this.authenticationManager = am;
        this.userDetailsService = uds;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userProgressService = userProgressService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            @CookieValue(value = "language", required = false) String languageCookie
    ) throws AlreadyInUseException, InvalidInputException {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(400).body(null);
        }

        if (userRepository.existsByUsername(username)) {
            throw new AlreadyInUseException(
                    String.format(ErrorMessages.USERNAME_ALREADY_EXIST.getMessage(), username));
        }

        if (userRepository.existsByEmail(email)) {
            throw new AlreadyInUseException(
                    String.format(ErrorMessages.EMAIL_ALREADY_EXIST.getMessage(), email));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        String chosen = (request.getInterfaceLanguage() != null && !request.getInterfaceLanguage().isBlank())
                ? request.getInterfaceLanguage()
                : (languageCookie != null && !languageCookie.isBlank() ? languageCookie : "en");
        user.setInterfaceLanguage(com.learningsystemserver.utils.LanguageUtils.normalize(chosen));

        user.setSubDifficultyLevel(0);
        user.setOverallProgressLevel(DifficultyLevel.BASIC);
        user.setOverallProgressScore(1.0);

        User saved = userRepository.save(user);

        userProgressService.seedForNewUser(saved.getId());

        String token = jwtService.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest req,
            @CookieValue(value = "language", required = false) String languageCookie
    ) {
        try {
            final String rawEmail = req.getEmail() == null ? "" : req.getEmail().trim();
            if (!EMAIL_PATTERN.matcher(rawEmail).matches()) {
                return ResponseEntity.status(401).body("Login requires a valid email address.");
            }

            var userOpt = userRepository.findByEmail(rawEmail);
            if (userOpt.isEmpty()) {
                throw new BadCredentialsException("Invalid credentials");
            }

            var username = userOpt.get().getUsername();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.getPassword())
            );

            if (languageCookie != null && !languageCookie.isBlank()) {
                var user = userOpt.get();
                String normalized = com.learningsystemserver.utils.LanguageUtils.normalize(languageCookie);
                if (!normalized.equals(user.getInterfaceLanguage())) {
                    user.setInterfaceLanguage(normalized);
                    userRepository.save(user);
                }
            }

            var userDetails = userDetailsService.loadUserByUsername(username);
            String access = jwtService.generateAccessToken(userDetails);
            String refresh = jwtService.generateRefreshToken(userDetails);

            var accessCookie = com.learningsystemserver.utils.CookieUtils.accessCookie(access, secureCookies);
            var refreshCookie = com.learningsystemserver.utils.CookieUtils.refreshCookie(refresh, secureCookies);

            var headers = new org.springframework.http.HttpHeaders();
            headers.add(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());
            headers.add(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString());

            String role = userDetails.getAuthorities().stream()
                    .findFirst().map(Object::toString).orElse("USER");

            return ResponseEntity.ok().headers(headers).body(new LoginResponse(role));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return ResponseEntity.status(401).build();

        String refresh = null;
        for (Cookie c : cookies) {
            if ("refresh_token".equals(c.getName())) {
                refresh = c.getValue();
                break;
            }
        }
        if (refresh == null) return ResponseEntity.status(401).build();

        try {
            if (!jwtService.isRefreshToken(refresh)) return ResponseEntity.status(401).build();
            String username = jwtService.extractUsername(refresh);
            var user = userDetailsService.loadUserByUsername(username);

            if (jwtService.isExpired(refresh)) return ResponseEntity.status(401).build();

            String access = jwtService.generateAccessToken(user);
            var accessCookie = CookieUtils.accessCookie(access, secureCookies);

            var headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
            return ResponseEntity.ok().headers(headers).build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.clear("access_token", secureCookies).toString());
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.clear("refresh_token", secureCookies).toString());
        return ResponseEntity.ok().headers(headers).build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");
        return ResponseEntity.ok(new LoginResponse(role));
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private final String role;
    }
}
