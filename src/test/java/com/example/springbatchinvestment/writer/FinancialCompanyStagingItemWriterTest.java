package com.example.springbatchinvestment.writer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyArea;
import com.example.springbatchinvestment.domain.CompanySyncItem;
import com.example.springbatchinvestment.domain.FinancialGroupType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FinancialCompanyStagingItemWriterTest {

    @Mock private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock private ObjectMapper objectMapper;

    private FinancialCompanyStagingItemWriter writer;

    @BeforeEach
    void setUp() {
        this.writer =
                new FinancialCompanyStagingItemWriter(
                        this.namedParameterJdbcTemplate, this.objectMapper);
    }

    @Test
    void 스테이징저장시_createdAt을utc로설정한다() throws Exception {
        Company company =
                new Company(
                        "202501",
                        "001",
                        "테스트은행",
                        "담당자",
                        "https://example.com",
                        "02-000-0000",
                        FinancialGroupType.BANK);
        CompanyArea companyArea = new CompanyArea("202501", "001", "01", "서울", "Y");
        CompanySyncItem companySyncItem = new CompanySyncItem(company, List.of(companyArea));

        when(this.objectMapper.writeValueAsString(any())).thenReturn("{}");

        this.writer.write(new Chunk<>(List.of(companySyncItem)));

        ArgumentCaptor<SqlParameterSource> parameterSourceCaptor =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(this.namedParameterJdbcTemplate, times(1))
                .update(anyString(), parameterSourceCaptor.capture());

        OffsetDateTime createdAt =
                (OffsetDateTime) parameterSourceCaptor.getValue().getValue("createdAt");
        org.assertj.core.api.Assertions.assertThat(createdAt.getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
