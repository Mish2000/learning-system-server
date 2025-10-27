package com.learningsystemserver.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StreamingOllamaService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public SseEmitter streamSolution(String questionText) {
        // 0L = no timeout; client controls lifecycle
        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            Map<String, Object> requestBody = Map.of(
                    "model", "aya-expanse:8b",
                    "prompt", "Q: " + questionText + "\nA:",
                    "stream", true,
                    "temperature", 0.6,
                    "repeat_penalty", 1.2
            );

            final String bodyString;
            try {
                bodyString = mapper.writeValueAsString(requestBody);
            } catch (Exception e) {
                completeGracefullyOnClientAbort(emitter, e);
                return;
            }

            Request req = new Request.Builder()
                    .url("http://localhost:11434/api/generate")
                    .post(RequestBody.create(bodyString, MediaType.parse("application/json")))
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    throw new IOException("Bad response from Ollama: " + (resp != null ? resp.code() : "null"));
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(resp.body().byteStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        // Ollama returns JSON lines
                        JsonNode node = mapper.readTree(line);
                        if (node.has("response")) {
                            String chunk = node.get("response").asText("");
                            if (!chunk.isEmpty()) {
                                sendChunk(emitter, chunk);
                            }
                        }
                        boolean done = node.has("done") && node.get("done").asBoolean(false);
                        if (done) {
                            log.info("Done = true from model, finishing SSE.");
                            break;
                        }
                    }
                }

                safeSendDone(emitter);
            } catch (Exception ex) {
                completeGracefullyOnClientAbort(emitter, ex);
            }
        }, "ollama-stream-thread").start();

        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String partial) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("chunk")
                    .data(partial);
            emitter.send(event);
        } catch (IOException e) {
            // If the client closed the connection, this is expected; don’t escalate it.
            if (isClientAbort(e)) {
                log.debug("Client disconnected while sending chunk: {}", e.getMessage());
                safeComplete(emitter);
            } else {
                log.error("Error sending SSE chunk", e);
                safeCompleteWithError(emitter, e);
            }
        }
    }

    private void safeSendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        } catch (IOException e) {
            if (isClientAbort(e)) {
                log.debug("Client disconnected before DONE could be sent: {}", e.getMessage());
            } else {
                log.warn("Failed to send DONE event", e);
            }
        } finally {
            safeComplete(emitter);
        }
    }

    private void completeGracefullyOnClientAbort(SseEmitter emitter, Exception ex) {
        if (isClientAbort(ex)) {
            // Typical messages: "Broken pipe", "Connection reset by peer", etc.
            log.debug("Client disconnected during SSE stream: {}", ex.getMessage());
            safeComplete(emitter);
        } else {
            log.error("Error in streaming SSE to Ollama", ex);
            safeCompleteWithError(emitter, ex);
        }
    }

    private boolean isClientAbort(Exception ex) {
        if (ex instanceof SocketException) return true;
        String msg = ex.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("broken pipe")
                || lower.contains("connection reset")
                || lower.contains("aborted")
                || lower.contains("closed")
                || lower.contains("peer");
    }

    private void safeComplete(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignore) {}
    }

    private void safeCompleteWithError(SseEmitter emitter, Exception ex) {
        try {
            // send a last error event only if the client is still connected
            emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
        } catch (Exception ignore) {}
        try { emitter.completeWithError(ex); } catch (Exception ignore) {}
    }
}
