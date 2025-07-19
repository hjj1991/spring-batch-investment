package com.example.springbatchinvestment;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.es.FinancialProductDocument;
import com.example.springbatchinvestment.processor.FinancialProductEsItemProcessor;
import com.example.springbatchinvestment.reader.FinancialCompanyItemReader;
import com.example.springbatchinvestment.reader.FinancialProductEsItemReader;
import com.example.springbatchinvestment.reader.FinancialProductItemReader;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import com.example.springbatchinvestment.tasklet.FinancialProductStatusUpdateTasklet;
import com.example.springbatchinvestment.writer.FinancialCompanyItemWriter;
import com.example.springbatchinvestment.writer.FinancialProductEsItemWriter;
import com.example.springbatchinvestment.writer.FinancialProductItemWriter;
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
    private static final String FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_ES_SYNC_STEP_NAME =
            "FINANCIAL_PRODUCT_ES_SYNC_STEP";
    private static final String FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME =
            "FINANCIAL_PRODUCT_STATUS_UPDATE_STEP";

    private final JobRepository jobRepository;
    private final FinancialCompanyRepository financialCompanyRepository;
    private final FinancialProductRepository financialProductRepository;
    private final JobExecutionListener jobLoggerListener;
    private final FinancialProductItemWriter financialProductItemWriter;
    private final FinancialProductEsItemWriter financialProductEsItemWriter;
    private final FinancialProductEsItemReader financialProductEsItemReader;
    private final FinancialProductEsItemProcessor financialProductEsItemProcessor;
    private final FinancialProductStatusUpdateTasklet financialProductStatusUpdateTasklet;

    @Value(value = "${api.fss.base-url}")
    private String baseUrl;

    @Value(value = "${api.fss.auth-key}")
    private String authKey;

    @Bean(name = FINANCIAL_COMPANY_SYNC_JOB_NAME)
    public Job financialSyncJob() {
        return new JobBuilder(FINANCIAL_COMPANY_SYNC_JOB_NAME, this.jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(this.financialSyncStep())
                .next(this.financialProductStatusUpdateStep()) // New step added
                .next(this.financialProductSavingsSyncStep())
                .next(this.financialProductInstallmentSavingsSyncStep())
                .next(this.financialProductEsSyncStep())
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

    @Bean(name = FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME)
    public Step financialProductStatusUpdateStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_STATUS_UPDATE_STEP_NAME, this.jobRepository)
                .tasklet(this.financialProductStatusUpdateTasklet, new ResourcelessTransactionManager())
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME)
    public Step financialProductSavingsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(
                        10, new ResourcelessTransactionManager())
                .reader(
                        new FinancialProductItemReader(
                                this.baseUrl, this.authKey, FinancialProductType.SAVINGS))
                .writer(this.financialProductItemWriter)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME)
    public Step financialProductInstallmentSavingsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductModel, FinancialProductModel>chunk(
                        10, new ResourcelessTransactionManager())
                .reader(
                        new FinancialProductItemReader(
                                this.baseUrl, this.authKey, FinancialProductType.INSTALLMENT_SAVINGS))
                .writer(this.financialProductItemWriter)
                .build();
    }

    @Bean(name = FINANCIAL_PRODUCT_ES_SYNC_STEP_NAME)
    public Step financialProductEsSyncStep() {
        return new StepBuilder(FINANCIAL_PRODUCT_ES_SYNC_STEP_NAME, this.jobRepository)
                .<FinancialProductEntity, FinancialProductDocument>chunk(
                        10, new ResourcelessTransactionManager())
                .reader(this.financialProductEsItemReader.financialProductEsReader())
                .processor(this.financialProductEsItemProcessor)
                .writer(this.financialProductEsItemWriter)
                .build();
    }
}
