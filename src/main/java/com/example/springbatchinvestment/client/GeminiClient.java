package com.example.springbatchinvestment.client;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GeminiClient {
    private final Client geminiApiClient;

    public GeminiClient(@Value("${api.gemini.auth-key}") String auth) {
        this.geminiApiClient = Client.builder().apiKey(auth).build();
    }

    public float[] embedContent(String text) {
        EmbedContentResponse response = this.geminiApiClient.models.embedContent("gemini-embedding-001", text, null);
        return response.embeddings()
                .map(contentEmbeddings -> {
                    List<Float> list = contentEmbeddings.getFirst().values().orElse(Collections.emptyList());
                    float[] arr = new float[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        arr[i] = list.get(i);
                    }
                    return arr;
                })
                .orElse(new float[0]);
    }

}
