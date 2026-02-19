package com.example.springbatchinvestment.writer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springbatchinvestment.domain.FinancialGroupType;
import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.InterestRateType;
import com.example.springbatchinvestment.domain.JoinRestriction;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FinancialProductHistoryPgmqItemWriterTest {

    @Mock private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ObjectMapper objectMapper;

    private FinancialProductHistoryPgmqItemWriter writer;

    @BeforeEach
    void setUp() {
        when(this.namedParameterJdbcTemplate.getJdbcOperations()).thenReturn(this.jdbcTemplate);
        this.writer =
                new FinancialProductHistoryPgmqItemWriter(
                        this.namedParameterJdbcTemplate, this.objectMapper);
    }

    @Test
    void 신규상품이면Queue이벤트를적재한다() throws Exception {
        FinancialProductEntity entity = this.createEntity();
        when(this.objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(this.namedParameterJdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(List.of());
        when(this.namedParameterJdbcTemplate.queryForObject(
                        anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(1L);

        this.writer.write(new Chunk<>(List.of(entity)));

        verify(this.namedParameterJdbcTemplate, times(2))
                .update(anyString(), any(SqlParameterSource.class));
        verify(this.namedParameterJdbcTemplate, times(1))
                .queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.namedParameterJdbcTemplate, times(2))
                .update(sqlCaptor.capture(), any(SqlParameterSource.class));
        org.assertj.core.api.Assertions.assertThat(sqlCaptor.getAllValues())
                .anyMatch(
                        sql ->
                                sql.contains(
                                        "ON CONFLICT (observed_at, financial_product_id, interest_rate_type, deposit_period_months)"));

        ArgumentCaptor<SqlParameterSource> parameterSourceCaptor =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(this.namedParameterJdbcTemplate, times(2))
                .update(anyString(), parameterSourceCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(parameterSourceCaptor.getAllValues())
                .allSatisfy(
                        parameterSource -> {
                            OffsetDateTime observedAt =
                                    (OffsetDateTime) parameterSource.getValue("observedAt");
                            org.assertj.core.api.Assertions.assertThat(observedAt.getOffset())
                                    .isEqualTo(ZoneOffset.UTC);
                        });
    }

    @Test
    void 변경이없으면Queue이벤트를적재하지않는다() throws Exception {
        FinancialProductEntity entity = this.createEntity();
        when(this.objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(this.namedParameterJdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "status", ProductStatus.ACTIVE.name(),
                                        "product_content_hash", "hash-1",
                                        "payload_equal", true)));

        this.writer.write(new Chunk<>(List.of(entity)));

        verify(this.namedParameterJdbcTemplate, never())
                .update(anyString(), any(SqlParameterSource.class));
        verify(this.namedParameterJdbcTemplate, never())
                .queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));
    }

    @Test
    void payload문자열순서가달라도_동일json이면_이력미적재한다() throws Exception {
        FinancialProductEntity entity = this.createEntity();
        when(this.objectMapper.writeValueAsString(any())).thenReturn("{\"a\":1,\"b\":2}");
        when(this.namedParameterJdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "status", ProductStatus.ACTIVE.name(),
                                        "product_content_hash", "hash-1",
                                        "payload_equal", true)));

        this.writer.write(new Chunk<>(List.of(entity)));

        verify(this.namedParameterJdbcTemplate, never())
                .update(anyString(), any(SqlParameterSource.class));
        verify(this.namedParameterJdbcTemplate, never())
                .queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));
    }

    private FinancialProductEntity createEntity() {
        FinancialCompanyEntity companyEntity =
                FinancialCompanyEntity.builder()
                        .financialCompanyId(1L)
                        .financialCompanyCode("001")
                        .dclsMonth("202501")
                        .companyName("테스트은행")
                        .financialGroupType(FinancialGroupType.BANK)
                        .build();

        FinancialProductEntity productEntity =
                FinancialProductEntity.builder()
                        .financialProductId(2L)
                        .financialCompanyEntity(companyEntity)
                        .financialProductCode("P-001")
                        .financialProductName("테스트상품")
                        .joinRestriction(JoinRestriction.NO_RESTRICTION)
                        .financialProductType(FinancialProductType.SAVINGS)
                        .joinMember("개인")
                        .additionalNotes("노트")
                        .status(ProductStatus.ACTIVE)
                        .productContentHash("hash-1")
                        .build();

        FinancialProductOptionEntity optionEntity =
                FinancialProductOptionEntity.builder()
                        .financialProductOptionId(3L)
                        .financialProductEntity(productEntity)
                        .dclsMonth("202501")
                        .interestRateType(InterestRateType.SIMPLE)
                        .interestRateTypeName("단리")
                        .depositPeriodMonths(12)
                        .baseInterestRate(BigDecimal.valueOf(2.5))
                        .maximumInterestRate(BigDecimal.valueOf(3.0))
                        .build();

        productEntity.getFinancialProductOptionEntities().add(optionEntity);
        return productEntity;
    }
}
