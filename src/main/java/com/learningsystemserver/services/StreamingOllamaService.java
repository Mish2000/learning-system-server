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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StreamingOllamaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "aya-expanse:8b";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Stream a solution from Ollama, enforcing the answer language
     * according to the provided UI language ("he" / "en").
     */
    public SseEmitter streamSolution(String questionText, String lang) {
        final String effectiveLang = normalizeLang(lang); // default handled inside
        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            // ---------- Language-aware preamble ----------
            String systemPreamble = buildSystemPrompt(effectiveLang);

            String finalPrompt;
            if ("en".equalsIgnoreCase(effectiveLang)) {
                finalPrompt =
                        systemPreamble +
                                "\n\nQuestion:\n" + (questionText == null ? "" : questionText) + "\n\nAnswer:";
            } else {
                // default Hebrew
                finalPrompt =
                        systemPreamble +
                                "\n\nשאלה:\n" + (questionText == null ? "" : questionText) + "\n\nתשובה בעברית:";
            }

            // Request body (keep original knobs + stop tokens)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("prompt", finalPrompt);
            requestBody.put("stream", true);
            requestBody.put("temperature", 0.6);
            requestBody.put("repeat_penalty", 1.2);
            requestBody.put("stop", List.of(
                    "Q:", "Question:", "שאלה:",
                    "Answer:", "A:",
                    "\nQ", "\nQuestion", "\nשאלה"
            ));

            final String bodyString;
            try {
                bodyString = mapper.writeValueAsString(requestBody);
            } catch (Exception e) {
                completeGracefullyOnClientAbort(emitter, e);
                return;
            }

            Request req = new Request.Builder()
                    .url(OLLAMA_URL)
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

    // ---- helpers ----

    private String normalizeLang(String v) {
        if (v == null) return "he";
        v = v.trim().toLowerCase();
        if (v.startsWith("en")) return "en";
        if (v.startsWith("he")) return "he";
        return "he";
    }

    private String buildSystemPrompt(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return String.join("\n",
                    "You are a math tutor. Respond ONLY in clear, natural English.",
                    "Use step-by-step explanations when relevant, concise and precise.",
                    "Keep proper spaces between words and around punctuation.",
                    "Use standard math symbols (+, −, ×, ÷, =, √, π).",
                    "Do NOT include any Hebrew or non-English words."
            );
        }
        // default -> Hebrew
        return String.join("\n",
                "את/ה מורה למתמטיקה. ענה/י אך ורק בעברית תקנית, ברורה וטבעית.",
                "אל תשלב/י בכלל מילים באנגלית. השתמש/י רק בסמלים מתמטיים סטנדרטיים (לדוגמה: +, −, ×, ÷, =, √, π).",
                "הצג/י פתרון שלב-אחר-שלב בצורה תמציתית ומדויקת.",
                "שמור/י על רווח תקין בין מילים וסימני פיסוק (כולל בין מספרים למילים).",
                "אין לשלב טקסט אנגלי או קיצורים באנגלית בשום מצב."
        );
    }

    private void sendChunk(SseEmitter emitter, String partial) {
        try {
            var payload = java.util.Map.of("t", partial);
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("chunk")
                    .data(payload)); // Spring will JSON-encode this map
        } catch (Exception e) {
            completeGracefullyOnClientAbort(emitter, e);
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
