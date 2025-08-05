package com.example.springbatchinvestment.client;

import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.HttpOptions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GeminiClient {
    private final List<String> apiKeys;
    private int currentKeyIndex = 0;
    private static final int MAX_RETRIES = 3;

    public GeminiClient(@Value("${api.gemini.auth-key}") String authKeys) {
        this.apiKeys = Arrays.asList(authKeys.split(","));
        if (this.apiKeys.isEmpty()) {
            throw new IllegalArgumentException("Gemini API keys must be provided.");
        }
    }

    private Client getGeminiApiClient() {
        return Client.builder().apiKey(this.apiKeys.get(this.currentKeyIndex)).build();
    }

    public float[] embedContent(String text) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                Client geminiApiClient = this.getGeminiApiClient();
                EmbedContentConfig embedContentConfig =
                        new EmbedContentConfig() {
                            @Override
                            public Optional<HttpOptions> httpOptions() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> taskType() {
                                return Optional.of("RETRIEVAL_DOCUMENT");
                            }

                            @Override
                            public Optional<String> title() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<Integer> outputDimensionality() {
                                return Optional.of(768);
                            }

                            @Override
                            public Optional<String> mimeType() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<Boolean> autoTruncate() {
                                return Optional.empty();
                            }

                            @Override
                            public Builder toBuilder() {
                                return null;
                            }
                        };

                EmbedContentResponse response =
                        geminiApiClient.models.embedContent("gemini-embedding-001", text, embedContentConfig);
                return response
                        .embeddings()
                        .map(
                                contentEmbeddings -> {
                                    List<Float> list =
                                            contentEmbeddings.getFirst().values().orElse(Collections.emptyList());
                                    float[] arr = new float[list.size()];
                                    for (int i = 0; i < list.size(); i++) {
                                        arr[i] = list.get(i);
                                    }
                                    return arr;
                                })
                        .orElse(new float[0]);
            } catch (ClientException e) {
                if (e.code() == 429) {
                    log.warn(
                            "API quota exceeded for key {}. Switching to next key.",
                            this.apiKeys.get(this.currentKeyIndex));
                    this.currentKeyIndex = (this.currentKeyIndex + 1) % this.apiKeys.size();
                    retryCount++; // Increment retryCount here
                    if (retryCount == MAX_RETRIES) { // Check if all retries are exhausted
                        log.error("All API keys exhausted. Failed to embed content after multiple retries.", e);
                        throw e; // Re-throw if all retries fail
                    }
                    // Loop will naturally re-evaluate condition
                } else {
                    log.error("Gemini API client error: {}", e.getMessage(), e);
                    throw e; // Re-throw for other client errors
                }
            } catch (Exception e) {
                log.error("An unexpected error occurred during embedding: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to embed content due to unexpected error.", e);
            }
        }
        throw new RuntimeException(
                "Failed to embed content after multiple retries."); // Should be reached if all retries fail
    }
}
