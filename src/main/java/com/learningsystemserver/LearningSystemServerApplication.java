package com.learningsystemserver;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LearningSystemServerApplication {

    private static final String[] DOTENV_KEYS = {
            "DATABASE_URL",
            "DATABASE_USERNAME",
            "DATABASE_PASSWORD",
            "JWT_SECRET",
            "FRONTEND_ORIGIN",
            "SECURITY_COOKIES_SECURE",
            "OLLAMA_BASE_URL",
            "OLLAMA_MODEL",
            "OLLAMA_AUTO_START"
    };

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        for (String key : DOTENV_KEYS) {
            setSystemPropertyIfPresent(dotenv, key);
        }
        SpringApplication.run(LearningSystemServerApplication.class, args);
    }

    private static void setSystemPropertyIfPresent(Dotenv dotenv, String key) {
        if (hasText(System.getProperty(key)) || hasText(System.getenv(key))) {
            return;
        }

        String value = dotenv.get(key);
        if (hasText(value)) {
            System.setProperty(key, value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
