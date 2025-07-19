package com.example.springbatchinvestment.service.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class GoogleEmbeddingService implements EmbeddingService {
    public static final String GEMINI_EMBEDDING_001 = "gemini-embedding-001";
    private final EmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        EmbeddingResponse resp = this.embeddingModel.call(
                new EmbeddingRequest(
                        List.of(text),
                        VertexAiTextEmbeddingOptions.builder()
                                .model(GEMINI_EMBEDDING_001)
                                .taskType(VertexAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT)
                                .build()
                )
        );
        return resp.getResult().getOutput();
    }
}
