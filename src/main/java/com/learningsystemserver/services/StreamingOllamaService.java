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
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StreamingOllamaService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public SseEmitter streamSolution(String questionText) {
        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            Map<String, Object> requestBody = Map.of(
                    "model", "aya-expanse:8b",
                    "prompt", "Q: " + questionText + "\nA:",
                    "stream", true,
                    "temperature", 0.6,
                    "repeat_penalty", 1.2
            );

            String bodyString;
            try {
                bodyString = mapper.writeValueAsString(requestBody);
            } catch (Exception e) {
                completeWithError(emitter, e);
                return;
            }

            Request request = new Request.Builder()
                    .url("http://localhost:11434/api/generate")
                    .post(RequestBody.create(bodyString, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unsuccessful response from Ollama: " + response);
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.replaceAll("\\r$", "");
                        if (line.isEmpty()) continue;

                        JsonNode node = mapper.readTree(line);
                        String partial = node.has("response") ? node.get("response").asText() : "";
                        boolean done = node.has("done") && node.get("done").asBoolean(false);

                        //todo
                       // log.info("Ollama chunk => \"{}\"", partial);

                        sendChunk(emitter, partial);

                        if (done) {
                            log.info("Done = true from model, finishing SSE.");
                            break;
                        }
                    }
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                }
            } catch (Exception ex) {
                completeWithError(emitter, ex);
            }
        }).start();

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
            log.error("Error sending SSE event", e);
            emitter.completeWithError(e);
        }
    }

    private void completeWithError(SseEmitter emitter, Exception ex) {
        log.error("Error in blocking SSE to Ollama", ex);
        try {
            emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
        } catch (IOException ignored) {}
        emitter.completeWithError(ex);
    }
}
