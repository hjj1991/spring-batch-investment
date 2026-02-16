package com.example.springbatchinvestment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SkipLoggerListener {

    @OnSkipInRead
    public void onSkipInRead(Throwable throwable) {
        log.warn("Skipped item while reading.", throwable);
    }

    @OnSkipInWrite
    public void onSkipInWrite(Object item, Throwable throwable) {
        log.warn("Skipped item while writing. item={}", item, throwable);
    }

    @OnSkipInProcess
    public void onSkipInProcess(Object item, Throwable throwable) {
        log.warn("Skipped item while processing. item={}", item, throwable);
    }
}
