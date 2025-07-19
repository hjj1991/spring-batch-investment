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
        return this.geminiClient.embedContent(text);
    }
}