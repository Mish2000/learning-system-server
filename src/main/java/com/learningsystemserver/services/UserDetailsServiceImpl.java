package com.learningsystemserver.services;

import com.learningsystemserver.entities.User;
import com.learningsystemserver.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("UserDetailsServiceImpl - loadUserByUsername: {}", username); // <-- ADD THIS

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found in DB for username: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        log.info("User found in DB: username={}, email={}, role={}",
                user.getUsername(), user.getEmail(), user.getRole());

        return new UserPrincipal(user);
    }
}
