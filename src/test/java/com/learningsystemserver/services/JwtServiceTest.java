package com.learningsystemserver.services;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String JWT_SECRET_ERROR =
            "JWT_SECRET must be set and contain at least 32 bytes for HS256 signing.";

    private final UserDetailsService userDetailsService = username -> {
        throw new UnsupportedOperationException("Not needed for JwtService construction tests");
    };

    @Test
    void constructorFailsClearlyWhenJwtSecretIsMissing() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> new JwtService(environment, userDetailsService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(JWT_SECRET_ERROR);
    }

    @Test
    void constructorFailsClearlyWhenJwtSecretIsTooShortForHs256() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("security.jwt.secret", "too-short");

        assertThatThrownBy(() -> new JwtService(environment, userDetailsService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(JWT_SECRET_ERROR);
    }

    @Test
    void constructorAcceptsJwtSecretWithAtLeastThirtyTwoBytes() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("security.jwt.secret", "12345678901234567890123456789012");

        assertThat(new JwtService(environment, userDetailsService)).isNotNull();
    }
}
