package com.learningsystemserver.services;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class OllamaBootstrapper implements ApplicationRunner {

    private static final String OLLAMA_HEALTH_URL = "http://localhost:11434/api/tags";
    private Process ollamaProcess;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isOllamaActive()) {
            log.info("Ollama is not active. Attempting to start: `ollama serve` ...");
            startOllama();
            waitUntilOllamaActive(Duration.ofSeconds(30));
        } else {
            log.info("Ollama is active.");
        }
    }

    private boolean isOllamaActive() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_HEALTH_URL))
                    .timeout(Duration.ofSeconds(2))
                    .GET().build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private void startOllama() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
            pb.redirectErrorStream(true);
            ollamaProcess = pb.start();
            log.info("Started `ollama serve` (pid: {}).", ollamaProcess.pid());

            Thread gobbler = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(ollamaProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        log.debug("[ollama] {}", line);
                    }
                } catch (IOException ioe) {
                    log.debug("Ollama output reader finished: {}", ioe.getMessage());
                }
            }, "ollama-stdout-gobbler");
            gobbler.setDaemon(true);
            gobbler.start();

        } catch (IOException e) {
            log.error("Failed to start `ollama serve`. Is Ollama installed and in PATH?", e);
        }
    }

    private void waitUntilOllamaActive(Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOllamaActive()) {
                log.info("Ollama service is now active.");
                return;
            }
            Thread.sleep(500);
        }
        log.warn("Timed out waiting for Ollama to become active.");
    }

    @PreDestroy
    public void shutdown() {
        if (ollamaProcess != null && ollamaProcess.isAlive()) {
            ollamaProcess.destroy();
            log.info("Stopped child `ollama serve` process.");
        }
    }
}
