package com.learningsystemserver.services;

import com.learningsystemserver.utils.CookieUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final boolean secureCookies;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService uds, boolean secureCookies) {
        this.jwtService = jwtService;
        this.userDetailsService = uds;
        this.secureCookies = secureCookies;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getRequestURI();

        // Do NOT skip /api/auth/me
        if (SKIP_PATHS.contains(path) || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String access = resolveToken(request);

        // If we have no access cookie/header — try sliding refresh using refresh cookie.
        if (!StringUtils.hasText(access)) {
            tryRefreshAndAuthenticate(request, response);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(access);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(access, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // Token present but not valid — try sliding refresh.
                    tryRefreshAndAuthenticate(request, response);
                }
            }
        } catch (ExpiredJwtException e) {
            // Access expired — try sliding refresh and proceed.
            tryRefreshAndAuthenticate(request, response);
        } catch (Exception ignored) {
            // Never break the chain on parsing errors.
        }

        filterChain.doFilter(request, response);
    }

    private void tryRefreshAndAuthenticate(HttpServletRequest request, HttpServletResponse response) {
        String refresh = resolveCookie(request);
        if (!StringUtils.hasText(refresh)) {
            return;
        }
        try {
            // Only accept real, non-expired refresh tokens
            if (!jwtService.isExpired(refresh)
                    && "refresh".equals(jwtService.extractClaim(refresh, c -> (String) c.get("typ")))) {

                String username = jwtService.extractUsername(refresh);
                var userDetails = userDetailsService.loadUserByUsername(username);

                // Mint new access
                String newAccess = jwtService.generateAccessToken(userDetails);
                var accessCookie = CookieUtils.accessCookie(newAccess, secureCookies);
                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ignored) {
            // If refresh is bad/expired — do nothing; downstream security will return 401.
        }
    }

    private String resolveToken(HttpServletRequest req) {
        // 1) Authorization header
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // 2) HttpOnly cookie set by backend
        if (req.getCookies() != null) {
            return Arrays.stream(req.getCookies())
                    .filter(c -> "access_token".equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    private String resolveCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}
