package com.learningsystemserver.controllers;

import com.learningsystemserver.dtos.AuthRequest;
import com.learningsystemserver.dtos.AuthResponse;
import com.learningsystemserver.dtos.RegisterRequest;
import com.learningsystemserver.entities.Role;
import com.learningsystemserver.entities.User;
import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.exceptions.UnauthorizedException;
import com.learningsystemserver.repositories.UserRepository;
import com.learningsystemserver.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.learningsystemserver.exceptions.ErrorMessages.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) throws AlreadyInUseException {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyInUseException(
                    String.format(USERNAME_ALREADY_EXIST.getMessage(), request.getUsername())
            );
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyInUseException(
                    String.format(EMAIL_ALREADY_EXIST.getMessage(), request.getEmail())
            );
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.USER);

        userRepository.save(newUser);

        String token = jwtService.generateToken(newUser.getUsername());
        return new AuthResponse(
                token,
                newUser.getUsername(),
                newUser.getRole().name()
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) throws  InvalidInputException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        User dbUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidInputException(
                        String.format(USERNAME_DOES_NOT_EXIST.getMessage(), request.getUsername())
                ));


        String token = jwtService.generateToken(dbUser.getUsername());
        return new AuthResponse(
                token,
                dbUser.getUsername(),
                dbUser.getRole().name()
        );
    }
}



