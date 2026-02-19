package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.databind.ObjectMapper;

public class FinancialProductStagingItemWriter implements ItemWriter<FinancialProductModel> {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final FinancialProductType financialProductType;

    public FinancialProductStagingItemWriter(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ObjectMapper objectMapper,
            FinancialProductType financialProductType) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.financialProductType = financialProductType;
    }

    @BeforeStep
    public void initializeStagingTable() {
        this.namedParameterJdbcTemplate
                .getJdbcOperations()
                .execute(
                        """
                        CREATE TABLE IF NOT EXISTS financial_product_staging (
                            id BIGSERIAL PRIMARY KEY,
                            financial_product_type VARCHAR(50) NOT NULL,
                            product_payload JSONB NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL
                        )
                        """);
        this.namedParameterJdbcTemplate.update(
                "DELETE FROM financial_product_staging WHERE financial_product_type = :financialProductType",
                new MapSqlParameterSource().addValue("financialProductType", this.financialProductType.name()));
    }

    @Override
    public void write(Chunk<? extends FinancialProductModel> chunk) throws Exception {
        List<? extends FinancialProductModel> items = chunk.getItems();
        for (FinancialProductModel item : items) {
            this.namedParameterJdbcTemplate.update(
                    """
                    INSERT INTO financial_product_staging(
                        financial_product_type,
                        product_payload,
                        created_at
                    ) VALUES (
                        :financialProductType,
                        CAST(:productPayload AS jsonb),
                        :createdAt
                    )
                    """,
                    new MapSqlParameterSource()
                            .addValue("financialProductType", this.financialProductType.name())
                            .addValue("productPayload", this.objectMapper.writeValueAsString(item))
                            .addValue("createdAt", OffsetDateTime.now(Clock.systemUTC())));
        }
    }
}
