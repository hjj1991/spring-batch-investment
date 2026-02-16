package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.databind.ObjectMapper;

public class FinancialProductStagingItemReader implements ItemStreamReader<FinancialProductModel> {

    private static final String EXECUTION_CONTEXT_LAST_READ_ID_KEY_PREFIX =
            "financialProductStagingItemReader.lastReadId.";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final FinancialProductType financialProductType;
    private final int pageSize;

    private long lastReadId = 0L;
    private List<FinancialProductModel> currentItems = List.of();
    private int currentIndex = 0;

    public FinancialProductStagingItemReader(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ObjectMapper objectMapper,
            FinancialProductType financialProductType,
            int pageSize) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.financialProductType = financialProductType;
        this.pageSize = pageSize;
    }

    @Override
    public FinancialProductModel read() throws Exception {
        if (this.currentIndex >= this.currentItems.size()) {
            this.currentItems = this.readPage();
            this.currentIndex = 0;
            if (this.currentItems.isEmpty()) {
                return null;
            }
        }

        return this.currentItems.get(this.currentIndex++);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.lastReadId = executionContext.getLong(this.getExecutionContextKey(), 0L);
        this.currentItems = List.of();
        this.currentIndex = 0;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(this.getExecutionContextKey(), this.lastReadId);
    }

    private List<FinancialProductModel> readPage() throws Exception {
        List<Map<String, Object>> rows =
                this.namedParameterJdbcTemplate.queryForList(
                        """
                        SELECT id, product_payload::text AS product_payload
                        FROM financial_product_staging
                        WHERE financial_product_type = :financialProductType
                          AND id > :lastReadId
                        ORDER BY id
                        LIMIT :pageSize
                        """,
                        new MapSqlParameterSource()
                                .addValue("financialProductType", this.financialProductType.name())
                                .addValue("lastReadId", this.lastReadId)
                                .addValue("pageSize", this.pageSize));

        List<FinancialProductModel> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Number id = (Number) row.get("id");
            this.lastReadId = id.longValue();
            result.add(this.objectMapper.readValue((String) row.get("product_payload"), FinancialProductModel.class));
        }

        return result;
    }

    private String getExecutionContextKey() {
        return EXECUTION_CONTEXT_LAST_READ_ID_KEY_PREFIX + this.financialProductType.name();
    }
}
