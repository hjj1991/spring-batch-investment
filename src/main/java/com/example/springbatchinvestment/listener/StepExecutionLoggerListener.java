package com.example.springbatchinvestment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepExecutionLoggerListener {

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step {} is starting.", stepExecution.getStepName());
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info(
                "Step {} finished. status={}, readCount={}, writeCount={}, readSkipCount={}, writeSkipCount={}, rollbackCount={}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getReadSkipCount(),
                stepExecution.getWriteSkipCount(),
                stepExecution.getRollbackCount());
        return stepExecution.getExitStatus();
    }
}
