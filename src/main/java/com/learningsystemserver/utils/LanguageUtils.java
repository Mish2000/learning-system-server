package com.learningsystemserver.utils;

public class LanguageUtils {
    private LanguageUtils() {}

    public static String normalize(String raw) {
        if (raw == null) return "en";
        String s = raw.trim();
        if (s.isEmpty()) return "en";

        String lower = s.toLowerCase();

        // Hebrew checks
        if (lower.equals("he") || lower.equals("iw") || lower.startsWith("heb")
                || "עברית".equals(s) || lower.contains("hebrew")) {
            return "he";
        }

        // English checks
        if (lower.equals("en") || lower.startsWith("eng") || lower.contains("english")
                || "אנגלית".equals(s)) {
            return "en";
        }

        if (lower.startsWith("he")) return "he";

        return "en";
    }
}
