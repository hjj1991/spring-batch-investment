package com.example.springbatchinvestment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemWriteLoggerListener {

    @OnWriteError
    public void onWriteError(Exception exception, Chunk<? extends Object> items) {
        log.error("Write failed. size={}", items.size(), exception);
    }
}
