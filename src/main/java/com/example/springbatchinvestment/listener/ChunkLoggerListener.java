package com.example.springbatchinvestment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.OnChunkError;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChunkLoggerListener {

    @OnChunkError
    public void onChunkError(Exception exception, Chunk<Object> outputs) {
        log.error("Chunk failed. outputSize={}", outputs.size(), exception);
    }
}
