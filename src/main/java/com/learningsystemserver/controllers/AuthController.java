package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.requests.RegisterRequest;
import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.ErrorMessages;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.JwtService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public AuthController(
            AuthenticationManager am,
            UserDetailsService uds,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = am;
        this.userDetailsService = uds;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) throws AlreadyInUseException {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String email = req.getEmail() == null ? "" : req.getEmail().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body("username, email and password are required");
        }

        if (userRepository.existsByUsername(username)) {
            throw new AlreadyInUseException(
                    String.format(ErrorMessages.USERNAME_ALREADY_EXIST.getMessage(), username));
        }

        if (userRepository.existsByEmail(email)) {
            throw new AlreadyInUseException(
                    String.format(ErrorMessages.EMAIL_ALREADY_EXIST.getMessage(), email));
        }
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(Role.USER);
        userRepository.save(u);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
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

            var userDetails = userDetailsService.loadUserByUsername(username);
            String access = jwtService.generateAccessToken(userDetails);
            String refresh = jwtService.generateRefreshToken(userDetails);

            var accessCookie  = CookieUtils.accessCookie(access,  secureCookies);
            var refreshCookie = CookieUtils.refreshCookie(refresh, secureCookies);

            var headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

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
