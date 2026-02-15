package com.example.springbatchinvestment;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.domain.CompanySyncItem;
import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.reader.FinancialCompanyItemReader;
import com.example.springbatchinvestment.reader.FinancialProductHistoryItemReader;
import com.example.springbatchinvestment.reader.FinancialProductItemReader;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.tasklet.FinancialProductStatusUpdateTasklet;
import com.example.springbatchinvestment.writer.FinancialCompanyItemWriter;
import com.example.springbatchinvestment.writer.FinancialProductHistoryPgmqItemWriter;
import com.example.springbatchinvestment.writer.FinancialProductItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class BachConfig {

    private static final int CHUNK_SIZE = 10;
    private static final ResourcelessTransactionManager TRANSACTION_MANAGER =
            new ResourcelessTransactionManager();

    private static final String FINANCIAL_COMPANY_SYNC_JOB_NAME = "FINANCIAL_COMPANY_SYNC_JOB";
    private static final String FINANCIAL_COMPANY_SYNC_STEP_NAME = "FINANCIAL_COMPANY_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME =
            "FINANCIAL_PRODUCT_STATUS_UPDATE_STEP";

    private final JobRepository jobRepository;
    private final FinancialCompanyRepository financialCompanyRepository;
    private final JobExecutionListener jobLoggerListener;
    private final FinancialProductItemWriter financialProductItemWriter;
    private final FinancialProductHistoryItemReader financialProductHistoryItemReader;
    private final FinancialProductHistoryPgmqItemWriter financialProductHistoryPgmqItemWriter;
    private final FinancialProductStatusUpdateTasklet financialProductStatusUpdateTasklet;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value(value = "${api.fss.base-url}")
    private String baseUrl;

    @Value(value = "${api.fss.auth-key}")
    private String authKey;

    @Bean(name = FINANCIAL_COMPANY_SYNC_JOB_NAME)
    public Job financialSyncJob() {
        return new JobBuilder(FINANCIAL_COMPANY_SYNC_JOB_NAME, this.jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(this.financialSyncStep())
                .next(this.financialProductSavingsSyncStep())
                .next(this.financialProductInstallmentSavingsSyncStep())
                .next(this.financialProductStatusUpdateStep())
                .next(this.financialProductHistoryPgmqSyncStep())
                .listener(this.jobLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_COMPANY_SYNC_STEP_NAME)
    public Step financialSyncStep() {
        return new StepBuilder(FINANCIAL_COMPANY_SYNC_STEP_NAME, this.jobRepository)
                .<CompanySyncItem, CompanySyncItem>chunk(CHUNK_SIZE)
                .transactionManager(TRANSACTION_MANAGER)
                .reader(new FinancialCompanyItemReader(this.baseUrl, this.authKey))
                .writer(
                        new FinancialCompanyItemWriter(
                                this.financialCompanyRepository,
                                this.namedParameterJdbcTemplate,
                                this.objectMapper))
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME)
    public Step financialProductStatusUpdateStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME, this.jobRepository)
                .tasklet(this.financialProductStatusUpdateTasklet, TRANSACTION_MANAGER)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME)
    public Step financialProductSavingsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(CHUNK_SIZE)
                .transactionManager(TRANSACTION_MANAGER)
                .reader(
                        new FinancialProductItemReader(
                                this.baseUrl, this.authKey, FinancialProductType.SAVINGS))
                .writer(this.financialProductItemWriter)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME)
    public Step financialProductInstallmentSavingsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(CHUNK_SIZE)
                .transactionManager(TRANSACTION_MANAGER)
                .reader(
                        new FinancialProductItemReader(
                                this.baseUrl, this.authKey, FinancialProductType.INSTALLMENT_SAVINGS))
                .writer(this.financialProductItemWriter)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP_NAME)
    public Step financialProductHistoryPgmqSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductEntity, FinancialProductEntity>chunk(CHUNK_SIZE)
                .transactionManager(TRANSACTION_MANAGER)
                .reader(this.financialProductHistoryItemReader.financialProductHistoryReader())
                .writer(this.financialProductHistoryPgmqItemWriter)
                .build();
    }
}
