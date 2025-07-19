package com.example.springbatchinvestment.service.embedding;

import com.example.springbatchinvestment.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleEmbeddingService implements EmbeddingService {

    private final GeminiClient geminiClient;

    @Override
    public float[] embed(String text) {
        try {
            // API 할당량 초과 방지를 위해 딜레이 추가 (예: 1000ms)
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Embedding API call interrupted.", e);
        }
        return this.geminiClient.embedContent(text);
    }
}