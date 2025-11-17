package com.learningsystemserver.utils;

public final class LanguageUtils {
    private LanguageUtils() {}

    public static String normalize(String raw) {
        if (raw == null) return "en";
        String s = raw.trim();
        if (s.isEmpty()) return "en";

        final String lower = s.toLowerCase();

        // Heb: codes & English label variants
        if (lower.equals("he") || lower.equals("iw") || lower.startsWith("heb") || lower.contains("hebrew") || containsHebrew(s)) {
            return "he";
        }

        // Eng: codes & English label variants (and Hebrew "אנגלית" if someone sent it)
        if (lower.equals("en") || lower.startsWith("eng") || lower.contains("english") || containsEnglishLetters(s)) {
            return "en";
        }

        // Fallback heuristics
        if (lower.startsWith("he")) return "he";
        return "en";
    }

    private static boolean containsHebrew(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '\u0590' && c <= '\u05FF') || (c >= '\uFB1D' && c <= '\uFB4F')) return true;
        }
        return false;
    }

    private static boolean containsEnglishLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return true;
        }
        return false;
    }
}
