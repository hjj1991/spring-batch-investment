package com.example.springbatchinvestment.tasklet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;

@ExtendWith(MockitoExtension.class)
class FinancialProductStatusUpdateTaskletTest {

    @Mock private FinancialProductRepository financialProductRepository;

    @InjectMocks private FinancialProductStatusUpdateTasklet financialProductStatusUpdateTasklet;

    @Test
    void 이번실행에서관측되지않은상품만삭제상태로변경한다() {
        StepContribution contribution = mock(StepContribution.class);
        StepExecution stepExecution = mock(StepExecution.class);
        JobExecution jobExecution = mock(JobExecution.class);
        ChunkContext chunkContext = mock(ChunkContext.class);
        LocalDateTime runStart = LocalDateTime.of(2026, 2, 15, 0, 0);
        ZonedDateTime expectedRunStartedAt = runStart.atZone(ZoneOffset.UTC);

        when(contribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getCreateTime()).thenReturn(runStart);

        this.financialProductStatusUpdateTasklet.execute(contribution, chunkContext);

        verify(this.financialProductRepository)
                .updateStatusForNotSeenProducts(
                        FinancialProductType.SAVINGS, ProductStatus.DELETED, expectedRunStartedAt);
        verify(this.financialProductRepository)
                .updateStatusForNotSeenProducts(
                        FinancialProductType.INSTALLMENT_SAVINGS,
                        ProductStatus.DELETED,
                        expectedRunStartedAt);
    }
}
