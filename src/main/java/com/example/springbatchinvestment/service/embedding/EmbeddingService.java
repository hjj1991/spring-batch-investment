package com.example.springbatchinvestment.service.embedding;

import java.util.List;

public interface EmbeddingService {
    List<Float> embed(String text);
}
