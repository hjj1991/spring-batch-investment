package com.example.springbatchinvestment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemReadLoggerListener {

    @OnReadError
    public void onReadError(Exception exception) {
        log.error("Read failed.", exception);
    }
}
