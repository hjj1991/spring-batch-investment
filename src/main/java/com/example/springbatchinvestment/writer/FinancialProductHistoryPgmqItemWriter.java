package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialProductHistoryPgmqItemWriter implements ItemWriter<FinancialProductEntity> {

    private static final String QUEUE_NAME = "product_change_events";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean queueInitialized = new AtomicBoolean(false);

    @Override
    public void write(Chunk<? extends FinancialProductEntity> chunk) throws Exception {
        this.initializeQueue();
        OffsetDateTime observedAt = OffsetDateTime.now();

        for (FinancialProductEntity financialProductEntity : chunk.getItems()) {
            this.insertProductHistory(financialProductEntity, observedAt);
            this.insertRateHistory(financialProductEntity, observedAt);
            this.enqueueProductEvent(financialProductEntity, observedAt);
        }
    }

    private void initializeQueue() {
        if (this.queueInitialized.compareAndSet(false, true)) {
            this.jdbcTemplate.execute("SELECT pgmq.create('product_change_events')");
        }
    }

    private void insertProductHistory(
            FinancialProductEntity financialProductEntity, OffsetDateTime observedAt)
            throws JsonProcessingException {
        this.jdbcTemplate.update(
                """
                INSERT INTO financial_product_history(
                    observed_at,
                    financial_product_id,
                    financial_company_id,
                    financial_product_code,
                    financial_product_type,
                    status,
                    product_content_hash,
                    payload
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                """,
                observedAt,
                financialProductEntity.getFinancialProductId(),
                financialProductEntity.getFinancialCompanyEntity().getFinancialCompanyId(),
                financialProductEntity.getFinancialProductCode(),
                financialProductEntity.getFinancialProductType().name(),
                financialProductEntity.getStatus().name(),
                financialProductEntity.getProductContentHash(),
                this.objectMapper.writeValueAsString(this.createHistoryPayload(financialProductEntity)));
    }

    private Map<String, Object> createHistoryPayload(FinancialProductEntity financialProductEntity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("financial_product_name", financialProductEntity.getFinancialProductName());
        payload.put("company_name", financialProductEntity.getFinancialCompanyEntity().getCompanyName());
        payload.put("join_way", financialProductEntity.getJoinWay());
        payload.put("special_condition", financialProductEntity.getSpecialCondition());
        payload.put("additional_notes", financialProductEntity.getAdditionalNotes());
        payload.put("join_restriction", financialProductEntity.getJoinRestriction().name());
        payload.put("max_limit", financialProductEntity.getMaxLimit());
        payload.put(
                "options",
                financialProductEntity.getFinancialProductOptionEntities().stream()
                        .map(this::createOptionPayload)
                        .toList());
        return payload;
    }

    private Map<String, Object> createOptionPayload(FinancialProductOptionEntity optionEntity) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("interest_rate_type", optionEntity.getInterestRateType().name());
        option.put("reserve_type", optionEntity.getReserveType() == null ? null : optionEntity.getReserveType().name());
        option.put("deposit_period_months", optionEntity.getDepositPeriodMonths());
        option.put("base_interest_rate", optionEntity.getBaseInterestRate());
        option.put("maximum_interest_rate", optionEntity.getMaximumInterestRate());
        return option;
    }

    private void insertRateHistory(
            FinancialProductEntity financialProductEntity, OffsetDateTime observedAt) {
        for (FinancialProductOptionEntity optionEntity :
                financialProductEntity.getFinancialProductOptionEntities()) {
            this.jdbcTemplate.update(
                    """
                    INSERT INTO financial_product_rate_history(
                        observed_at,
                        financial_product_id,
                        financial_product_option_id,
                        interest_rate_type,
                        reserve_type,
                        deposit_period_months,
                        base_interest_rate,
                        maximum_interest_rate
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    observedAt,
                    financialProductEntity.getFinancialProductId(),
                    optionEntity.getFinancialProductOptionId(),
                    optionEntity.getInterestRateType().name(),
                    optionEntity.getReserveType() == null ? null : optionEntity.getReserveType().name(),
                    optionEntity.getDepositPeriodMonths(),
                    optionEntity.getBaseInterestRate(),
                    optionEntity.getMaximumInterestRate());
        }
    }

    private void enqueueProductEvent(
            FinancialProductEntity financialProductEntity, OffsetDateTime observedAt)
            throws JsonProcessingException {
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("event_type", "PRODUCT_SYNCED");
        eventPayload.put("occurred_at", observedAt);
        eventPayload.put("financial_product_id", financialProductEntity.getFinancialProductId());
        eventPayload.put(
                "financial_company_id",
                financialProductEntity.getFinancialCompanyEntity().getFinancialCompanyId());
        eventPayload.put("financial_product_code", financialProductEntity.getFinancialProductCode());
        eventPayload.put("status", financialProductEntity.getStatus().name());

        this.jdbcTemplate.queryForObject(
                "SELECT * FROM pgmq.send(?::text, CAST(? AS jsonb))",
                Long.class,
                QUEUE_NAME,
                this.objectMapper.writeValueAsString(eventPayload));
    }
}
