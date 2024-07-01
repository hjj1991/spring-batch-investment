package com.example.springbatchinvestment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobLoggerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job {} is starting.", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.error(
                    "Job {} failed with status {}.",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus());
        } else {
            log.info(
                    "Job {} completed successfully with status {}.",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus());
        }
    }
}
