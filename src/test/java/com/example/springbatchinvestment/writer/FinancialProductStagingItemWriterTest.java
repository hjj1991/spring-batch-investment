package com.example.springbatchinvestment.writer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springbatchinvestment.domain.FinancialGroupType;
import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
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
class FinancialProductStagingItemWriterTest {

    @Mock private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock private ObjectMapper objectMapper;

    private FinancialProductStagingItemWriter writer;

    @BeforeEach
    void setUp() {
        this.writer =
                new FinancialProductStagingItemWriter(
                        this.namedParameterJdbcTemplate,
                        this.objectMapper,
                        FinancialProductType.SAVINGS);
    }

    @Test
    void 스테이징저장시_createdAt을utc로설정한다() throws Exception {
        FinancialProductModel model =
                new FinancialProductModel(
                        "202501",
                        "001",
                        "P-001",
                        "테스트은행",
                        "테스트상품",
                        "영업점",
                        null,
                        null,
                        "1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        FinancialGroupType.BANK,
                        List.of(),
                        FinancialProductType.SAVINGS);
        when(this.objectMapper.writeValueAsString(model)).thenReturn("{}");

        this.writer.write(new Chunk<>(List.of(model)));

        ArgumentCaptor<SqlParameterSource> parameterSourceCaptor =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(this.namedParameterJdbcTemplate, times(1))
                .update(anyString(), parameterSourceCaptor.capture());

        OffsetDateTime createdAt =
                (OffsetDateTime) parameterSourceCaptor.getValue().getValue("createdAt");
        org.assertj.core.api.Assertions.assertThat(createdAt.getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
