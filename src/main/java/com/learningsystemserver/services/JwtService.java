package com.learningsystemserver.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final UserDetailsService userDetailsService;

    @Value("${security.jwt.access-minutes:15}")
    private long accessMinutes;

    @Value("${security.jwt.refresh-days:7}")
    private long refreshDays;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Lazy UserDetailsService userDetailsService
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.userDetailsService = userDetailsService;
    }

    public String generateAccessToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .addClaims(Map.of("role", user.getAuthorities().stream().findFirst().map(Object::toString).orElse("USER")))
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(accessMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS)))
                .claim("typ", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Backward-compat for older call sites (e.g., ProfileController)
    public String generateToken(String username) {
        UserDetails user = userDetailsService.loadUserByUsername(username);
        return generateAccessToken(user);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails user) {
        final String username = extractUsername(token);
        return username.equalsIgnoreCase(user.getUsername()) && !isExpired(token);
    }

    public boolean isExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims c = parseClaims(token);
            Object typ = c.get("typ");
            return typ != null && "refresh".equals(typ.toString());
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // Return the claims even when expired so we can read the subject safely when needed
            return e.getClaims();
        }
    }
}
