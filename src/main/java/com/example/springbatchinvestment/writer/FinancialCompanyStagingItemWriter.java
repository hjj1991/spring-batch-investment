package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.CompanySyncItem;
import java.util.List;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.databind.ObjectMapper;

public class FinancialCompanyStagingItemWriter implements ItemWriter<CompanySyncItem> {

    private static final String STAGING_TABLE_NAME = "financial_company_staging";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    public FinancialCompanyStagingItemWriter(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @BeforeStep
    public void initializeStagingTable() {
        this.namedParameterJdbcTemplate
                .getJdbcOperations()
                .execute(
                        """
                        CREATE TABLE IF NOT EXISTS financial_company_staging (
                            id BIGSERIAL PRIMARY KEY,
                            company_payload JSONB NOT NULL,
                            company_areas_payload JSONB NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """);
        this.namedParameterJdbcTemplate.getJdbcOperations().execute("TRUNCATE TABLE " + STAGING_TABLE_NAME);
    }

    @Override
    public void write(Chunk<? extends CompanySyncItem> chunk) throws Exception {
        List<? extends CompanySyncItem> items = chunk.getItems();
        for (CompanySyncItem item : items) {
            this.namedParameterJdbcTemplate.update(
                    """
                    INSERT INTO financial_company_staging(
                        company_payload,
                        company_areas_payload
                    ) VALUES (
                        CAST(:companyPayload AS jsonb),
                        CAST(:companyAreasPayload AS jsonb)
                    )
                    """,
                    new MapSqlParameterSource()
                            .addValue("companyPayload", this.objectMapper.writeValueAsString(item.company()))
                            .addValue(
                                    "companyAreasPayload",
                                    this.objectMapper.writeValueAsString(item.companyAreas())));
        }
    }
}
