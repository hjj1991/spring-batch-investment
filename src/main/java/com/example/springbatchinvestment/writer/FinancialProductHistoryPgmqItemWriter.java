package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class FinancialProductHistoryPgmqItemWriter implements ItemWriter<FinancialProductEntity> {

    private static final String QUEUE_NAME = "product_change_events";
    private static final String EVENT_TYPE_NEW_PRODUCT = "NEW_PRODUCT";
    private static final String EVENT_TYPE_STATUS_CHANGED = "STATUS_CHANGED";
    private static final String EVENT_TYPE_CONTENT_CHANGED = "CONTENT_CHANGED";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean queueInitialized = new AtomicBoolean(false);

    @Override
    public void write(Chunk<? extends FinancialProductEntity> chunk) throws Exception {
        this.initializeQueue();
        OffsetDateTime observedAt = OffsetDateTime.now();

        for (FinancialProductEntity financialProductEntity : chunk.getItems()) {
            String currentPayload =
                    this.objectMapper.writeValueAsString(this.createHistoryPayload(financialProductEntity));
            PreviousProductSnapshot previousSnapshot = this.findPreviousSnapshot(financialProductEntity);
            if (this.isUnchanged(financialProductEntity, previousSnapshot, currentPayload)) {
                continue;
            }

            String eventType = this.resolveEventType(financialProductEntity, previousSnapshot, currentPayload);
            this.insertProductHistory(financialProductEntity, observedAt, currentPayload);
            this.insertRateHistory(financialProductEntity, observedAt);
            if (eventType != null) {
                this.enqueueProductEvent(financialProductEntity, observedAt, eventType);
            }
        }
    }

    private PreviousProductSnapshot findPreviousSnapshot(FinancialProductEntity financialProductEntity) {
        List<Map<String, Object>> rows =
                this.namedParameterJdbcTemplate.queryForList(
                        """
                        SELECT status, product_content_hash, payload
                        FROM financial_product_history
                        WHERE financial_product_id = :financialProductId
                        ORDER BY observed_at DESC
                        LIMIT 1
                        """,
                        new MapSqlParameterSource()
                                .addValue("financialProductId", financialProductEntity.getFinancialProductId()));

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> previousRow = rows.get(0);
        return new PreviousProductSnapshot(
                (String) previousRow.get("status"),
                (String) previousRow.get("product_content_hash"),
                previousRow.get("payload") == null ? null : previousRow.get("payload").toString());
    }

    private boolean isUnchanged(
            FinancialProductEntity financialProductEntity,
            PreviousProductSnapshot previousSnapshot,
            String currentPayload) {
        if (previousSnapshot == null) {
            return false;
        }

        String currentStatus = financialProductEntity.getStatus().name();
        String currentHash = financialProductEntity.getProductContentHash();
        return Objects.equals(previousSnapshot.status(), currentStatus)
                && Objects.equals(previousSnapshot.productContentHash(), currentHash)
                && Objects.equals(previousSnapshot.payload(), currentPayload);
    }

    private String resolveEventType(
            FinancialProductEntity financialProductEntity,
            PreviousProductSnapshot previousSnapshot,
            String currentPayload) {
        if (previousSnapshot == null) {
            return EVENT_TYPE_NEW_PRODUCT;
        }
        String currentStatus = financialProductEntity.getStatus().name();
        String currentHash = financialProductEntity.getProductContentHash();

        if (!Objects.equals(previousSnapshot.status(), currentStatus)) {
            return EVENT_TYPE_STATUS_CHANGED;
        }
        if (!Objects.equals(previousSnapshot.productContentHash(), currentHash)
                || !Objects.equals(previousSnapshot.payload(), currentPayload)) {
            return EVENT_TYPE_CONTENT_CHANGED;
        }
        return null;
    }

    private void initializeQueue() {
        if (this.queueInitialized.compareAndSet(false, true)) {
            this.namedParameterJdbcTemplate
                    .getJdbcOperations()
                    .execute("SELECT pgmq.create('product_change_events')");
        }
    }

    private void insertProductHistory(
            FinancialProductEntity financialProductEntity, OffsetDateTime observedAt, String currentPayload) {
        this.namedParameterJdbcTemplate.update(
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
                ) VALUES (
                    :observedAt,
                    :financialProductId,
                    :financialCompanyId,
                    :financialProductCode,
                    :financialProductType,
                    :status,
                    :productContentHash,
                    CAST(:payload AS jsonb)
                )
                """,
                new MapSqlParameterSource()
                        .addValue("observedAt", observedAt)
                        .addValue("financialProductId", financialProductEntity.getFinancialProductId())
                        .addValue(
                                "financialCompanyId",
                                financialProductEntity.getFinancialCompanyEntity().getFinancialCompanyId())
                        .addValue("financialProductCode", financialProductEntity.getFinancialProductCode())
                        .addValue(
                                "financialProductType",
                                financialProductEntity.getFinancialProductType().name())
                        .addValue("status", financialProductEntity.getStatus().name())
                        .addValue("productContentHash", financialProductEntity.getProductContentHash())
                        .addValue("payload", currentPayload));
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
                        .sorted(
                                Comparator.comparing(
                                                (FinancialProductOptionEntity optionEntity) ->
                                                        optionEntity.getInterestRateType().name())
                                        .thenComparing(
                                                optionEntity ->
                                                        optionEntity.getReserveType() == null
                                                                ? ""
                                                                : optionEntity.getReserveType().name())
                                        .thenComparing(FinancialProductOptionEntity::getDepositPeriodMonths))
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
            this.namedParameterJdbcTemplate.update(
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
                    ) VALUES (
                        :observedAt,
                        :financialProductId,
                        :financialProductOptionId,
                        :interestRateType,
                        :reserveType,
                        :depositPeriodMonths,
                        :baseInterestRate,
                        :maximumInterestRate
                    )
                    ON CONFLICT (observed_at, financial_product_id, interest_rate_type, deposit_period_months)
                    DO UPDATE SET
                        financial_product_option_id = EXCLUDED.financial_product_option_id,
                        reserve_type = EXCLUDED.reserve_type,
                        base_interest_rate = EXCLUDED.base_interest_rate,
                        maximum_interest_rate = EXCLUDED.maximum_interest_rate
                    """,
                    new MapSqlParameterSource()
                            .addValue("observedAt", observedAt)
                            .addValue("financialProductId", financialProductEntity.getFinancialProductId())
                            .addValue(
                                    "financialProductOptionId",
                                    optionEntity.getFinancialProductOptionId())
                            .addValue("interestRateType", optionEntity.getInterestRateType().name())
                            .addValue(
                                    "reserveType",
                                    optionEntity.getReserveType() == null
                                            ? null
                                            : optionEntity.getReserveType().name())
                            .addValue("depositPeriodMonths", optionEntity.getDepositPeriodMonths())
                            .addValue("baseInterestRate", optionEntity.getBaseInterestRate())
                            .addValue("maximumInterestRate", optionEntity.getMaximumInterestRate()));
        }
    }

    private void enqueueProductEvent(
            FinancialProductEntity financialProductEntity, OffsetDateTime observedAt, String eventType)
            throws JacksonException {
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("event_type", eventType);
        eventPayload.put("occurred_at", observedAt);
        eventPayload.put("financial_product_id", financialProductEntity.getFinancialProductId());
        eventPayload.put(
                "financial_company_id",
                financialProductEntity.getFinancialCompanyEntity().getFinancialCompanyId());
        eventPayload.put("financial_product_code", financialProductEntity.getFinancialProductCode());
        eventPayload.put("status", financialProductEntity.getStatus().name());

        this.namedParameterJdbcTemplate.queryForObject(
                "SELECT * FROM pgmq.send(:queueName::text, CAST(:payload AS jsonb))",
                new MapSqlParameterSource()
                        .addValue("queueName", QUEUE_NAME)
                        .addValue("payload", this.objectMapper.writeValueAsString(eventPayload)),
                Long.class);
    }

    private record PreviousProductSnapshot(String status, String productContentHash, String payload) {}
}
