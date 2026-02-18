package com.example.springbatchinvestment.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.example.springbatchinvestment.domain.FinancialGroupType;
import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.JoinRestriction;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import com.example.springbatchinvestment.service.embedding.EmbeddingService;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBatchTest
@Testcontainers
@SpringBootTest(
        properties = {
            "spring.profiles.active=test",
            "spring.batch.job.enabled=false",
            "spring.batch.jdbc.initialize-schema=always",
            "spring.jpa.hibernate.ddl-auto=create",
            "api.fss.auth-key=test-key",
            "api.gemini.auth-key=test-key"
        })
class FinancialSyncBatchIntegrationTest {

    private static final String COMPANY_STEP = "FINANCIAL_COMPANY_SYNC_STEP";
    private static final String COMPANY_FETCH_STEP = "FINANCIAL_COMPANY_FETCH_STEP";
    private static final String SAVINGS_FETCH_STEP = "FINANCIAL_PRODUCT_SAVINGS_FETCH_STEP";
    private static final String SAVINGS_STEP = "FINANCIAL_PRODUCT_SAVINGS_SYNC_STEP";
    private static final String INSTALLMENT_FETCH_STEP = "FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_FETCH_STEP";
    private static final String INSTALLMENT_STEP = "FINANCIAL_PRODUCT_INSTALLMENT_SAVINGS_SYNC_STEP";
    private static final String STATUS_STEP = "FINANCIAL_PRODUCT_STATUS_UPDATE_STEP";
    private static final String HISTORY_STEP = "FINANCIAL_PRODUCT_HISTORY_PGMQ_SYNC_STEP";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer("pgvector/pgvector:pg16") {
                        @Override
                        protected void containerIsStarted(InspectContainerResponse containerInfo) {
                            super.containerIsStarted(containerInfo);
                            try {
                                var result =
                                        this.execInContainer(
                                                "psql",
                                                "-U",
                                                "postgres",
                                                "-d",
                                                "batch_test",
                                                "-c",
                                                "CREATE EXTENSION IF NOT EXISTS vector");
                                if (result.getExitCode() != 0) {
                                    throw new IllegalStateException(
                                            "Failed to create vector extension: " + result.getStderr());
                                }
                            } catch (Exception exception) {
                                throw new RuntimeException(exception);
                            }
                        }
                    }
                    .withDatabaseName("batch_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final MockWebServer mockWebServer = new MockWebServer();

    static {
        try {
            mockWebServer.start();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("api.fss.base-url", () -> mockWebServer.url("/").toString());
    }

    @AfterAll
    static void afterAll() throws Exception {
        mockWebServer.shutdown();
    }

    @Autowired private ApplicationContext applicationContext;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobRepositoryTestUtils jobRepositoryTestUtils;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FinancialCompanyRepository financialCompanyRepository;
    @Autowired private FinancialProductRepository financialProductRepository;

    @BeforeEach
    void setUp() {
        this.createBatchMetadataTables();
        this.jobRepositoryTestUtils.removeJobExecutions();
        this.convertLobColumnsToText();
        this.createAdditionalTables();
        this.cleanupData();
    }

    private void createBatchMetadataTables() {
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS batch_job_instance (
                    job_instance_id BIGINT NOT NULL PRIMARY KEY,
                    version BIGINT,
                    job_name VARCHAR(100) NOT NULL,
                    job_key VARCHAR(32) NOT NULL,
                    CONSTRAINT job_inst_un UNIQUE (job_name, job_key)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS batch_job_execution (
                    job_execution_id BIGINT NOT NULL PRIMARY KEY,
                    version BIGINT,
                    job_instance_id BIGINT NOT NULL,
                    create_time TIMESTAMP NOT NULL,
                    start_time TIMESTAMP DEFAULT NULL,
                    end_time TIMESTAMP DEFAULT NULL,
                    status VARCHAR(10),
                    exit_code VARCHAR(2500),
                    exit_message VARCHAR(2500),
                    last_updated TIMESTAMP,
                    CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id)
                        REFERENCES batch_job_instance(job_instance_id)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS batch_job_execution_params (
                    job_execution_id BIGINT NOT NULL,
                    parameter_name VARCHAR(100) NOT NULL,
                    parameter_type VARCHAR(100) NOT NULL,
                    parameter_value VARCHAR(2500),
                    identifying CHAR(1) NOT NULL,
                    CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id)
                        REFERENCES batch_job_execution(job_execution_id)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS batch_step_execution (
                    step_execution_id BIGINT NOT NULL PRIMARY KEY,
                    version BIGINT NOT NULL,
                    step_name VARCHAR(100) NOT NULL,
                    job_execution_id BIGINT NOT NULL,
                    create_time TIMESTAMP NOT NULL,
                    start_time TIMESTAMP DEFAULT NULL,
                    end_time TIMESTAMP DEFAULT NULL,
                    status VARCHAR(10),
                    commit_count BIGINT,
                    read_count BIGINT,
                    filter_count BIGINT,
                    write_count BIGINT,
                    read_skip_count BIGINT,
                    write_skip_count BIGINT,
                    process_skip_count BIGINT,
                    rollback_count BIGINT,
                    exit_code VARCHAR(2500),
                    exit_message VARCHAR(2500),
                    last_updated TIMESTAMP,
                    CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id)
                        REFERENCES batch_job_execution(job_execution_id)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS batch_step_execution_context (
                    step_execution_id BIGINT NOT NULL PRIMARY KEY,
                    short_context VARCHAR(2500) NOT NULL,
                    serialized_context TEXT,
                    CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id)
                        REFERENCES batch_step_execution(step_execution_id)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS batch_job_execution_context (
                    job_execution_id BIGINT NOT NULL PRIMARY KEY,
                    short_context VARCHAR(2500) NOT NULL,
                    serialized_context TEXT,
                    CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id)
                        REFERENCES batch_job_execution(job_execution_id)
                )
                """);
        this.jdbcTemplate.execute(
                "CREATE SEQUENCE IF NOT EXISTS batch_step_execution_seq MAXVALUE 9223372036854775807 NO CYCLE");
        this.jdbcTemplate.execute(
                "CREATE SEQUENCE IF NOT EXISTS batch_job_execution_seq MAXVALUE 9223372036854775807 NO CYCLE");
        this.jdbcTemplate.execute(
                "CREATE SEQUENCE IF NOT EXISTS batch_job_instance_seq MAXVALUE 9223372036854775807 NO CYCLE");
    }

    @Test
    void 금융회사배치스텝이_회사와지역을_적재한다() throws Exception {
        this.enqueueCompanyResponses();
        BatchStatus fetchStatus = this.executeStep(COMPANY_FETCH_STEP);
        assertThat(fetchStatus).isEqualTo(BatchStatus.COMPLETED);

        BatchStatus status = this.executeStep(COMPANY_STEP);

        assertThat(status).isEqualTo(BatchStatus.COMPLETED);
        assertThat(this.financialCompanyRepository.count()).isGreaterThan(0);
        Integer areaCount = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM financial_company_area", Integer.class);
        assertThat(areaCount).isNotNull();
        assertThat(areaCount).isGreaterThan(0);
    }

    @Test
    void 예금상품배치스텝이_상품을_적재한다() throws Exception {
        this.prepareCompanyData();
        this.enqueueSavingsResponses();
        BatchStatus fetchStatus = this.executeStep(SAVINGS_FETCH_STEP);
        assertThat(fetchStatus).isEqualTo(BatchStatus.COMPLETED);

        BatchStatus status = this.executeStep(SAVINGS_STEP);

        assertThat(status).isEqualTo(BatchStatus.COMPLETED);
        Integer productCount =
                this.jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM financial_product
                        WHERE financial_product_code = 'PRDT-001'
                          AND financial_product_type = 'SAVINGS'
                        """,
                        Integer.class);
        assertThat(productCount).isNotNull();
        assertThat(productCount).isGreaterThan(0);
    }

    @Test
    void 적금상품배치스텝이_상품을_적재한다() throws Exception {
        this.prepareCompanyData();
        this.enqueueInstallmentResponses();
        BatchStatus fetchStatus = this.executeStep(INSTALLMENT_FETCH_STEP);
        assertThat(fetchStatus).isEqualTo(BatchStatus.COMPLETED);

        BatchStatus status = this.executeStep(INSTALLMENT_STEP);

        assertThat(status).isEqualTo(BatchStatus.COMPLETED);
        Integer productCount =
                this.jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM financial_product
                        WHERE financial_product_code = 'PRDT-002'
                          AND financial_product_type = 'INSTALLMENT_SAVINGS'
                        """,
                        Integer.class);
        assertThat(productCount).isNotNull();
        assertThat(productCount).isGreaterThan(0);
    }

    @Test
    void 상태업데이트스텝이_관측되지않은상품을_DELETED로_변경한다() throws Exception {
        FinancialCompanyEntity company =
                this.financialCompanyRepository.save(
                        FinancialCompanyEntity.builder()
                                .financialCompanyCode("0010001")
                                .dclsMonth("202501")
                                .companyName("테스트은행")
                                .financialGroupType(FinancialGroupType.BANK)
                                .build());

        FinancialProductEntity product =
                FinancialProductEntity.builder()
                        .financialCompanyEntity(company)
                        .financialProductCode("STALE-001")
                        .dclsMonth("202501")
                        .financialProductName("오래된상품")
                        .joinRestriction(JoinRestriction.NO_RESTRICTION)
                        .financialProductType(FinancialProductType.SAVINGS)
                        .joinMember("개인")
                        .additionalNotes("메모")
                        .status(ProductStatus.ACTIVE)
                        .lastSeenAt(ZonedDateTime.now().minusDays(2))
                        .build();
        this.financialProductRepository.save(product);

        BatchStatus status = this.executeStep(STATUS_STEP);

        assertThat(status).isEqualTo(BatchStatus.COMPLETED);
        FinancialProductEntity reloaded = this.financialProductRepository.findAll().get(0);
        assertThat(reloaded.getStatus()).isEqualTo(ProductStatus.DELETED);
    }

    @Test
    void 이력동기화스텝이_이력과금리이력을_적재한다() throws Exception {
        FinancialCompanyEntity company =
                this.financialCompanyRepository.save(
                        FinancialCompanyEntity.builder()
                                .financialCompanyCode("0010001")
                                .dclsMonth("202501")
                                .companyName("테스트은행")
                                .financialGroupType(FinancialGroupType.BANK)
                                .build());

        FinancialProductEntity product =
                FinancialProductEntity.builder()
                        .financialCompanyEntity(company)
                        .financialProductCode("HIS-001")
                        .dclsMonth("202501")
                        .financialProductName("이력상품")
                        .joinRestriction(JoinRestriction.NO_RESTRICTION)
                        .financialProductType(FinancialProductType.SAVINGS)
                        .joinMember("개인")
                        .additionalNotes("메모")
                        .status(ProductStatus.ACTIVE)
                        .productContentHash("hash-001")
                        .lastSeenAt(ZonedDateTime.now())
                        .build();
        FinancialProductOptionEntity option =
                FinancialProductOptionEntity.builder()
                        .financialProductEntity(product)
                        .dclsMonth("202501")
                        .interestRateType(com.example.springbatchinvestment.domain.InterestRateType.SIMPLE)
                        .interestRateTypeName("단리")
                        .depositPeriodMonths(12)
                        .baseInterestRate(BigDecimal.valueOf(2.1))
                        .maximumInterestRate(BigDecimal.valueOf(2.8))
                        .build();
        product.getFinancialProductOptionEntities().add(option);
        this.financialProductRepository.save(product);

        BatchStatus status = this.executeStep(HISTORY_STEP);

        assertThat(status).isEqualTo(BatchStatus.COMPLETED);
        Integer historyCount = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM financial_product_history", Integer.class);
        Integer rateHistoryCount = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM financial_product_rate_history", Integer.class);
        assertThat(historyCount).isNotNull();
        assertThat(historyCount).isGreaterThan(0);
        assertThat(rateHistoryCount).isNotNull();
        assertThat(rateHistoryCount).isGreaterThan(0);
    }

    private void prepareCompanyData() throws Exception {
        this.enqueueCompanyResponses();
        this.executeStep(COMPANY_FETCH_STEP);
        this.executeStep(COMPANY_STEP);
        this.jdbcTemplate.execute("UPDATE financial_company SET source_payload = NULL");
    }

    private BatchStatus executeStep(String stepName) throws Exception {
        Step step = this.applicationContext.getBean(stepName, Step.class);
        Job job = new JobBuilder("TEST_" + stepName + "_JOB", this.jobRepository).start(step).build();
        JobParameters jobParameters =
                new JobParametersBuilder().addLong("ts", System.nanoTime()).toJobParameters();
        JobInstance jobInstance = this.jobRepository.createJobInstance(job.getName(), jobParameters);
        JobExecution jobExecution =
                this.jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
        job.execute(jobExecution);
        return jobExecution.getStatus();
    }

    private void enqueueCompanyResponses() {
        this.mockJsonResponse(this.companyResponseJson("0010001", "우리은행", "01", "서울"));
        this.mockJsonResponse(this.emptyCompanyResponseJson());
    }

    private void enqueueSavingsResponses() {
        this.mockJsonResponse(this.productResponseJson("PRDT-001", "예금상품A", "D"));
        this.mockJsonResponse(this.emptyProductResponseJson());
    }

    private void enqueueInstallmentResponses() {
        this.mockJsonResponse(this.productResponseJson("PRDT-002", "적금상품B", "S"));
        this.mockJsonResponse(this.emptyProductResponseJson());
    }

    private void mockJsonResponse(String body) {
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body));
    }

    private void createAdditionalTables() {
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS financial_company_area (
                    financial_company_id BIGINT NOT NULL,
                    area_code VARCHAR(20) NOT NULL,
                    area_name VARCHAR(255) NOT NULL,
                    is_available BOOLEAN NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (financial_company_id, area_code)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS financial_product_history (
                    observed_at TIMESTAMPTZ NOT NULL,
                    financial_product_id BIGINT NOT NULL,
                    financial_company_id BIGINT NOT NULL,
                    financial_product_code VARCHAR(100) NOT NULL,
                    financial_product_type VARCHAR(50) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    product_content_hash VARCHAR(64),
                    payload JSONB NOT NULL,
                    PRIMARY KEY (observed_at, financial_product_id)
                )
                """);
        this.jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS financial_product_rate_history (
                    observed_at TIMESTAMPTZ NOT NULL,
                    financial_product_id BIGINT NOT NULL,
                    financial_product_option_id BIGINT,
                    interest_rate_type VARCHAR(50) NOT NULL,
                    reserve_type VARCHAR(50),
                    deposit_period_months SMALLINT NOT NULL,
                    base_interest_rate NUMERIC(8,5),
                    maximum_interest_rate NUMERIC(8,5),
                    payload JSONB,
                    PRIMARY KEY (observed_at, financial_product_id, interest_rate_type, deposit_period_months)
                )
                """);
        this.jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS pgmq");
        this.jdbcTemplate.execute(
                """
                CREATE OR REPLACE FUNCTION pgmq.create(queue_name TEXT)
                RETURNS VOID
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    RETURN;
                END;
                $$
                """);
        this.jdbcTemplate.execute(
                """
                CREATE OR REPLACE FUNCTION pgmq.send(queue_name TEXT, payload JSONB)
                RETURNS BIGINT
                LANGUAGE sql
                AS $$
                SELECT 1::BIGINT;
                $$
                """);
    }

    private void convertLobColumnsToText() {
        this.jdbcTemplate.execute("ALTER TABLE financial_company ALTER COLUMN source_payload TYPE TEXT");
        this.jdbcTemplate.execute("ALTER TABLE financial_product ALTER COLUMN additional_notes TYPE TEXT");
        this.jdbcTemplate.execute("ALTER TABLE financial_product ALTER COLUMN post_maturity_interest_rate TYPE TEXT");
        this.jdbcTemplate.execute("ALTER TABLE financial_product ALTER COLUMN source_payload TYPE TEXT");
        this.jdbcTemplate.execute("ALTER TABLE financial_product ALTER COLUMN special_condition TYPE TEXT");
        this.jdbcTemplate.execute("ALTER TABLE financial_product_option ALTER COLUMN source_payload TYPE TEXT");
    }

    private void cleanupData() {
        this.jdbcTemplate.execute("DROP TABLE IF EXISTS financial_product_staging");
        this.jdbcTemplate.execute("DROP TABLE IF EXISTS financial_company_staging");
        this.jdbcTemplate.execute("TRUNCATE TABLE financial_product_rate_history");
        this.jdbcTemplate.execute("TRUNCATE TABLE financial_product_history");
        this.jdbcTemplate.execute("TRUNCATE TABLE financial_company_area");
        this.jdbcTemplate.execute("TRUNCATE TABLE financial_product_option CASCADE");
        this.jdbcTemplate.execute("TRUNCATE TABLE financial_product CASCADE");
        this.jdbcTemplate.execute("TRUNCATE TABLE financial_company CASCADE");
    }

    private String companyResponseJson(
            String financialCompanyCode, String companyName, String areaCode, String areaName) {
        return """
                {
                  "result": {
                    "prdt_div": "C",
                    "err_cd": "000",
                    "err_msg": "정상",
                    "total_count": 1,
                    "max_page_no": 1,
                    "now_page_no": 1,
                    "baseList": [
                      {
                        "dcls_month": "202501",
                        "fin_co_no": "%s",
                        "kor_co_nm": "%s",
                        "dcls_chrg_man": "담당자",
                        "homp_url": "https://example.com",
                        "cal_tel": "010-0000-0000"
                      }
                    ],
                    "optionList": [
                      {
                        "dcls_month": "202501",
                        "fin_co_no": "%s",
                        "area_cd": "%s",
                        "area_nm": "%s",
                        "exis_yn": "Y"
                      }
                    ]
                  }
                }
                """
                .formatted(financialCompanyCode, companyName, financialCompanyCode, areaCode, areaName);
    }

    private String emptyCompanyResponseJson() {
        return """
                {
                  "result": {
                    "prdt_div": "C",
                    "err_cd": "000",
                    "err_msg": "정상",
                    "total_count": 0,
                    "max_page_no": 1,
                    "now_page_no": 1,
                    "baseList": [],
                    "optionList": []
                  }
                }
                """;
    }

    private String productResponseJson(String productCode, String productName, String reserveType) {
        return """
                {
                  "result": {
                    "prdt_div": "P",
                    "err_cd": "000",
                    "err_msg": "정상",
                    "total_count": 1,
                    "max_page_no": 1,
                    "now_page_no": 1,
                    "baseList": [
                      {
                        "dcls_month": "202501",
                        "fin_co_no": "0010001",
                        "fin_prdt_cd": "%s",
                        "kor_co_nm": "우리은행",
                        "fin_prdt_nm": "%s",
                        "join_way": "영업점",
                        "mtrt_int": "만기시",
                        "spcl_cnd": "우대금리",
                        "join_deny": "1",
                        "join_member": "개인",
                        "etc_note": "비고",
                        "max_limit": "10000000",
                        "dcls_strt_day": "20250101",
                        "dcls_end_day": "20251231",
                        "fin_co_subm_day": "2025-01-01 00:00:00"
                      }
                    ],
                    "optionList": [
                      {
                        "dcls_month": "202501",
                        "fin_co_no": "0010001",
                        "fin_prdt_cd": "%s",
                        "intr_rate_type": "S",
                        "intr_rate_type_nm": "단리",
                        "rsrv_type": "%s",
                        "rsrv_type_nm": "정액",
                        "save_trm": "12",
                        "intr_rate": 2.1,
                        "intr_rate2": 2.8
                      }
                    ]
                  }
                }
                """
                .formatted(productCode, productName, productCode, reserveType);
    }

    private String emptyProductResponseJson() {
        return """
                {
                  "result": {
                    "prdt_div": "P",
                    "err_cd": "000",
                    "err_msg": "정상",
                    "total_count": 0,
                    "max_page_no": 1,
                    "now_page_no": 1,
                    "baseList": [],
                    "optionList": []
                  }
                }
                """;
    }

    @TestConfiguration
    static class BatchTestConfig {
        @Bean
        @Primary
        EmbeddingService embeddingService() {
            return text -> new float[] {0.1f, 0.2f, 0.3f};
        }
    }
}
