// src/main/java/com/learningsystemserver/utils/CookieUtils.java
package com.learningsystemserver.utils;

import org.springframework.http.ResponseCookie;
import java.time.Duration;

public class CookieUtils {

    public static ResponseCookie accessCookie(String value, boolean secure) {
        return ResponseCookie.from("access_token", value)
                .httpOnly(true)
                .secure(secure)       // false in dev, true in prod
                .sameSite("Lax")      // same-origin in dev => Lax
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .build();
    }

    public static ResponseCookie refreshCookie(String value, boolean secure) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secure)       // false in dev, true in prod
                .sameSite("Lax")      // same-origin in dev => Lax
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    public static ResponseCookie clear(String name, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
