package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyArea;
import com.example.springbatchinvestment.domain.CompanySyncItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class FinancialCompanyStagingItemReader implements ItemStreamReader<CompanySyncItem> {

    private static final TypeReference<List<CompanyArea>> COMPANY_AREAS_TYPE_REFERENCE = new TypeReference<>() {};
    private static final String EXECUTION_CONTEXT_LAST_READ_ID_KEY =
            "financialCompanyStagingItemReader.lastReadId";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int pageSize;

    private long lastReadId = 0L;
    private List<CompanySyncItem> currentItems = List.of();
    private int currentIndex = 0;

    public FinancialCompanyStagingItemReader(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper, int pageSize) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.pageSize = pageSize;
    }

    @Override
    public CompanySyncItem read() {
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
        this.lastReadId = executionContext.getLong(EXECUTION_CONTEXT_LAST_READ_ID_KEY, 0L);
        this.currentItems = List.of();
        this.currentIndex = 0;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(EXECUTION_CONTEXT_LAST_READ_ID_KEY, this.lastReadId);
    }

    private List<CompanySyncItem> readPage() {
        List<Map<String, Object>> rows =
                this.namedParameterJdbcTemplate.queryForList(
                        """
                        SELECT id, company_payload::text AS company_payload, company_areas_payload::text AS company_areas_payload
                        FROM financial_company_staging
                        WHERE id > :lastReadId
                        ORDER BY id
                        LIMIT :pageSize
                        """,
                        new MapSqlParameterSource()
                                .addValue("lastReadId", this.lastReadId)
                                .addValue("pageSize", this.pageSize));

        List<CompanySyncItem> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Number id = (Number) row.get("id");
            this.lastReadId = id.longValue();

            Company company = this.objectMapper.readValue((String) row.get("company_payload"), Company.class);
            List<CompanyArea> companyAreas =
                    this.objectMapper.readValue((String) row.get("company_areas_payload"), COMPANY_AREAS_TYPE_REFERENCE);
            result.add(new CompanySyncItem(company, companyAreas));
        }

        return result;
    }
}
