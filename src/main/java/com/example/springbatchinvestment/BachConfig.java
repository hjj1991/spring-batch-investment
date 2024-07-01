package com.example.springbatchinvestment;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.reader.FinancialCompanyItemReader;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.writer.FinancialCompanyItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BachConfig {

    private static final String FINANCIAL_COMPANY_SYNC_JOB_NAME = "FINANCIAL_COMPANY_SYNC_JOB";
    private static final String FINANCIAL_COMPANY_SYNC_STEP_NAME = "FINANCIAL_COMPANY_SYNC_STEP";
    private final JobRepository jobRepository;
    private final FinancialCompanyRepository financialCompanyRepository;
    private final JobExecutionListener jobLoggerListener;

    @Value(value = "${api.fss.base-url}")
    private String baseUrl;

    @Value(value = "${api.fss.auth-key}")
    private String authKey;

    @Bean(name = FINANCIAL_COMPANY_SYNC_JOB_NAME)
    public Job financialSyncJob() {
        return new JobBuilder(FINANCIAL_COMPANY_SYNC_JOB_NAME, this.jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(this.financialSyncStep())
                .listener(this.jobLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_COMPANY_SYNC_STEP_NAME)
    public Step financialSyncStep() {
        return new StepBuilder(FINANCIAL_COMPANY_SYNC_STEP_NAME, this.jobRepository)
                .<Company, Company>chunk(10, new ResourcelessTransactionManager())
                .reader(new FinancialCompanyItemReader(this.baseUrl, this.authKey))
                .writer(new FinancialCompanyItemWriter(this.financialCompanyRepository))
                .build();
    }
}
