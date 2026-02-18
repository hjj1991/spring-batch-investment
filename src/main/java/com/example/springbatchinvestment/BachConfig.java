package com.example.springbatchinvestment;

import com.example.springbatchinvestment.client.error.FssUnavailableError;
import com.example.springbatchinvestment.domain.CompanySyncItem;
import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.listener.*;
import com.example.springbatchinvestment.reader.*;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.tasklet.FinancialProductStatusUpdateTasklet;
import com.example.springbatchinvestment.writer.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
@EnableJdbcJobRepository
public class BachConfig {

    private static final int CHUNK_SIZE = 10;
    private static final int FETCH_STEP_RETRY_LIMIT = 2;
    private static final int DB_STEP_RETRY_LIMIT = 3;
    private static final ResourcelessTransactionManager FETCH_STEP_TRANSACTION_MANAGER =
            new ResourcelessTransactionManager();

    private static final String FINANCIAL_COMPANY_SYNC_JOB_NAME = "FINANCIAL_COMPANY_SYNC_JOB";
    private static final String FINANCIAL_COMPANY_FETCH_STEP_NAME = "FINANCIAL_COMPANY_FETCH_STEP";
    private static final String FINANCIAL_COMPANY_SYNC_STEP_NAME = "FINANCIAL_COMPANY_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_SAVINGS_FETCH_STEP_NAME =
            "FINANCIAL_PRODUCT_SAVINGS_FETCH_STEP";
    private static final String FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_FETCH_STEP_NAME =
            "FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_FETCH_STEP";
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
    private final PlatformTransactionManager transactionManager;
    private final StepExecutionLoggerListener stepExecutionLoggerListener;
    private final ChunkLoggerListener chunkLoggerListener;
    private final ItemReadLoggerListener itemReadLoggerListener;
    private final ItemWriteLoggerListener itemWriteLoggerListener;
    private final SkipLoggerListener skipLoggerListener;

    @Value(value = "${api.fss.base-url}")
    private String baseUrl;

    @Value(value = "${api.fss.auth-key}")
    private String authKey;

    @Bean(name = FINANCIAL_COMPANY_SYNC_JOB_NAME)
    public Job financialSyncJob() {
        return new JobBuilder(FINANCIAL_COMPANY_SYNC_JOB_NAME, this.jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(this.financialCompanyFetchStep())
                .next(this.financialSyncStep())
                .next(this.financialProductSavingsFetchStep())
                .next(this.financialProductSavingsSyncStep())
                .next(this.financialProductInstallmentSavingsFetchStep())
                .next(this.financialProductInstallmentSavingsSyncStep())
                .next(this.financialProductStatusUpdateStep())
                .next(this.financialProductHistoryPgmqSyncStep())
                .listener(this.jobLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_COMPANY_FETCH_STEP_NAME)
    public Step financialCompanyFetchStep() {
        return new StepBuilder(FINANCIAL_COMPANY_FETCH_STEP_NAME, this.jobRepository)
                .<CompanySyncItem, CompanySyncItem>chunk(CHUNK_SIZE)
                .transactionManager(FETCH_STEP_TRANSACTION_MANAGER)
                .reader(new FinancialCompanyItemReader(this.baseUrl, this.authKey))
                .writer(new FinancialCompanyStagingItemWriter(this.namedParameterJdbcTemplate, this.objectMapper))
                .faultTolerant()
                .retryLimit(FETCH_STEP_RETRY_LIMIT)
                .retry(FssUnavailableError.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_COMPANY_SYNC_STEP_NAME)
    public Step financialSyncStep() {
        return new StepBuilder(FINANCIAL_COMPANY_SYNC_STEP_NAME, this.jobRepository)
                .<CompanySyncItem, CompanySyncItem>chunk(CHUNK_SIZE)
                .transactionManager(this.transactionManager)
                .reader(
                        new FinancialCompanyStagingItemReader(
                                this.namedParameterJdbcTemplate, this.objectMapper, CHUNK_SIZE))
                .writer(
                        new FinancialCompanyItemWriter(
                                this.financialCompanyRepository,
                                this.namedParameterJdbcTemplate,
                                this.objectMapper))
                .faultTolerant()
                .retryLimit(DB_STEP_RETRY_LIMIT)
                .retry(CannotAcquireLockException.class)
                .retry(PessimisticLockingFailureException.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME)
    public Step financialProductStatusUpdateStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME, this.jobRepository)
                .tasklet(this.financialProductStatusUpdateTasklet, this.transactionManager)
                .listener(this.stepExecutionLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME)
    public Step financialProductSavingsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(CHUNK_SIZE)
                .transactionManager(this.transactionManager)
                .reader(
                        new FinancialProductStagingItemReader(
                                this.namedParameterJdbcTemplate,
                                this.objectMapper,
                                FinancialProductType.SAVINGS,
                                CHUNK_SIZE))
                .writer(this.financialProductItemWriter)
                .faultTolerant()
                .retryLimit(DB_STEP_RETRY_LIMIT)
                .retry(CannotAcquireLockException.class)
                .retry(PessimisticLockingFailureException.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_SAVINGS_FETCH_STEP_NAME)
    public Step financialProductSavingsFetchStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_SAVINGS_FETCH_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(CHUNK_SIZE)
                .transactionManager(FETCH_STEP_TRANSACTION_MANAGER)
                .reader(
                        new FinancialProductItemReader(
                                this.baseUrl, this.authKey, FinancialProductType.SAVINGS))
                .writer(
                        new FinancialProductStagingItemWriter(
                                this.namedParameterJdbcTemplate,
                                this.objectMapper,
                                FinancialProductType.SAVINGS))
                .faultTolerant()
                .retryLimit(FETCH_STEP_RETRY_LIMIT)
                .retry(FssUnavailableError.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME)
    public Step financialProductInstallmentSavingsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(CHUNK_SIZE)
                .transactionManager(this.transactionManager)
                .reader(
                        new FinancialProductStagingItemReader(
                                this.namedParameterJdbcTemplate,
                                this.objectMapper,
                                FinancialProductType.INSTALLMENT_SAVINGS,
                                CHUNK_SIZE))
                .writer(this.financialProductItemWriter)
                .faultTolerant()
                .retryLimit(DB_STEP_RETRY_LIMIT)
                .retry(CannotAcquireLockException.class)
                .retry(PessimisticLockingFailureException.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_FETCH_STEP_NAME)
    public Step financialProductInstallmentSavingsFetchStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_FETCH_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(CHUNK_SIZE)
                .transactionManager(FETCH_STEP_TRANSACTION_MANAGER)
                .reader(
                        new FinancialProductItemReader(
                                this.baseUrl, this.authKey, FinancialProductType.INSTALLMENT_SAVINGS))
                .writer(
                        new FinancialProductStagingItemWriter(
                                this.namedParameterJdbcTemplate,
                                this.objectMapper,
                                FinancialProductType.INSTALLMENT_SAVINGS))
                .faultTolerant()
                .retryLimit(FETCH_STEP_RETRY_LIMIT)
                .retry(FssUnavailableError.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP_NAME)
    public Step financialProductHistoryPgmqSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductEntity, FinancialProductEntity>chunk(CHUNK_SIZE)
                .transactionManager(this.transactionManager)
                .reader(this.financialProductHistoryItemReader.financialProductHistoryReader())
                .writer(this.financialProductHistoryPgmqItemWriter)
                .faultTolerant()
                .retryLimit(DB_STEP_RETRY_LIMIT)
                .retry(CannotAcquireLockException.class)
                .retry(PessimisticLockingFailureException.class)
                .listener(this.stepExecutionLoggerListener)
                .listener(this.chunkLoggerListener)
                .listener(this.itemReadLoggerListener)
                .listener(this.itemWriteLoggerListener)
                .listener(this.skipLoggerListener)
                .build();
    }
}
