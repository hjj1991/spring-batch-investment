package com.example.springbatchinvestment.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springbatchinvestment.domain.FinancialGroupType;
import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import com.example.springbatchinvestment.service.embedding.EmbeddingService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

@ExtendWith(MockitoExtension.class)
class FinancialProductItemWriterTest {

    @Mock private FinancialProductRepository financialProductRepository;
    @Mock private FinancialCompanyRepository financialCompanyRepository;
    @Mock private EmbeddingService embeddingService;

    @Test
    void 임베딩이비활성화면_null벡터로저장하고_API를호출하지않는다() throws Exception {
        FinancialProductItemWriter writer =
                new FinancialProductItemWriter(
                        this.financialProductRepository,
                        this.financialCompanyRepository,
                        this.embeddingService,
                        false);
        this.initializeRunStartedAt(writer);

        FinancialProductModel model = this.createFinancialProductModel("PRDT-001");
        FinancialCompanyEntity companyEntity = this.createFinancialCompanyEntity();

        when(this.financialProductRepository
                        .findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCodeAndFinancialProductType(
                                model.finCoNo(), model.finPrdtCd(), model.financialProductType()))
                .thenReturn(Optional.empty());
        when(this.financialCompanyRepository.findByFinancialCompanyCode(model.finCoNo()))
                .thenReturn(Optional.of(companyEntity));

        writer.write(new Chunk<>(List.of(model)));

        ArgumentCaptor<FinancialProductEntity> entityCaptor =
                ArgumentCaptor.forClass(FinancialProductEntity.class);
        verify(this.financialProductRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEmbeddingVector()).isNull();
        verify(this.embeddingService, never()).embed(any());
    }

    @Test
    void 임베딩이활성화면_벡터를저장한다() throws Exception {
        FinancialProductItemWriter writer =
                new FinancialProductItemWriter(
                        this.financialProductRepository,
                        this.financialCompanyRepository,
                        this.embeddingService,
                        true);
        this.initializeRunStartedAt(writer);

        FinancialProductModel model = this.createFinancialProductModel("PRDT-002");
        FinancialCompanyEntity companyEntity = this.createFinancialCompanyEntity();
        float[] embedding = new float[] {0.1f, 0.2f};

        when(this.financialProductRepository
                        .findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCodeAndFinancialProductType(
                                model.finCoNo(), model.finPrdtCd(), model.financialProductType()))
                .thenReturn(Optional.empty());
        when(this.financialCompanyRepository.findByFinancialCompanyCode(model.finCoNo()))
                .thenReturn(Optional.of(companyEntity));
        when(this.embeddingService.embed(any())).thenReturn(embedding);

        writer.write(new Chunk<>(List.of(model)));

        ArgumentCaptor<FinancialProductEntity> entityCaptor =
                ArgumentCaptor.forClass(FinancialProductEntity.class);
        verify(this.financialProductRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEmbeddingVector()).isEqualTo(embedding);
        verify(this.embeddingService).embed(any());
    }

    private void initializeRunStartedAt(FinancialProductItemWriter writer) {
        StepExecution stepExecution = org.mockito.Mockito.mock(StepExecution.class);
        JobExecution jobExecution = org.mockito.Mockito.mock(JobExecution.class);

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getCreateTime()).thenReturn(LocalDateTime.of(2026, 2, 16, 0, 0));

        writer.captureRunStartedAt(stepExecution);
    }

    private FinancialProductModel createFinancialProductModel(String financialProductCode) {
        return new FinancialProductModel(
                "202602",
                "0010001",
                financialProductCode,
                "테스트은행",
                "테스트상품",
                "인터넷",
                "만기 후 0.5%",
                "우대조건",
                "1",
                "누구나",
                "비고",
                "1000000",
                "20260201",
                "20261231",
                "2026-02-16 00:00:00",
                FinancialGroupType.BANK,
                List.of(),
                FinancialProductType.SAVINGS);
    }

    private FinancialCompanyEntity createFinancialCompanyEntity() {
        return FinancialCompanyEntity.builder()
                .financialCompanyCode("0010001")
                .dclsMonth("202602")
                .companyName("테스트은행")
                .financialGroupType(FinancialGroupType.BANK)
                .build();
    }
}
