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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.cookies.secure:false}")
    private boolean secureCookies;

    public AuthController(
            AuthenticationManager am,
            UserDetailsService uds,
            JwtService jwtService,
            UserRepository userRepository,        // ⬅ add
            PasswordEncoder passwordEncoder       // ⬅ add
    ) {
        this.authenticationManager = am;
        this.userDetailsService = uds;
        this.jwtService = jwtService;
        this.userRepository = userRepository;     // ⬅ add
        this.passwordEncoder = passwordEncoder;   // ⬅ add
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
        u.setRole(Role.USER); // default role
        userRepository.save(u);

        // We return 200 OK with no cookies (user logs in afterward).
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            var auth = new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword());
            authenticationManager.authenticate(auth);

            var user = userDetailsService.loadUserByUsername(req.getEmail());
            String access = jwtService.generateAccessToken(user);
            String refresh = jwtService.generateRefreshToken(user);

            var accessCookie = CookieUtils.accessCookie(access, secureCookies);
            var refreshCookie = CookieUtils.refreshCookie(refresh, secureCookies);

            var headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            String role = user.getAuthorities().stream().findFirst().map(Object::toString).orElse("USER");
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
