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
import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
class FinancialCompanyItemWriterTest {

    @Mock private FinancialCompanyRepository financialCompanyRepository;
    @Mock private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock private ObjectMapper objectMapper;

    private FinancialCompanyItemWriter writer;

    @BeforeEach
    void setUp() {
        this.writer =
                new FinancialCompanyItemWriter(
                        this.financialCompanyRepository,
                        this.namedParameterJdbcTemplate,
                        this.objectMapper);
    }

    @Test
    void 회사지역저장시_utc시간파라미터를사용한다() throws Exception {
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

        when(this.financialCompanyRepository.findByFinancialCompanyCode("001"))
                .thenReturn(Optional.empty());
        when(this.objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(this.financialCompanyRepository.save(any(FinancialCompanyEntity.class)))
                .thenReturn(FinancialCompanyEntity.builder().financialCompanyId(1L).build());

        this.writer.write(new Chunk<>(List.of(companySyncItem)));

        ArgumentCaptor<SqlParameterSource> parameterSourceCaptor =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(this.namedParameterJdbcTemplate, times(1))
                .update(anyString(), parameterSourceCaptor.capture());

        OffsetDateTime currentUtcDateTime =
                (OffsetDateTime) parameterSourceCaptor.getValue().getValue("currentUtcDateTime");
        org.assertj.core.api.Assertions.assertThat(currentUtcDateTime.getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
