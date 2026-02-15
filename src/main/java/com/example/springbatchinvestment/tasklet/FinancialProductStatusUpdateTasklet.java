package com.example.springbatchinvestment.tasklet;

import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FinancialProductStatusUpdateTasklet implements Tasklet {

    private final FinancialProductRepository financialProductRepository;

    @Override
    @Transactional
    public RepeatStatus execute(
            @NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) {
        ZonedDateTime runStartedAt =
                contribution.getStepExecution().getJobExecution().getCreateTime().atZone(ZoneOffset.UTC);

        this.financialProductRepository.updateStatusForNotSeenProducts(
                FinancialProductType.SAVINGS, ProductStatus.DELETED, runStartedAt);

        this.financialProductRepository.updateStatusForNotSeenProducts(
                FinancialProductType.INSTALLMENT_SAVINGS, ProductStatus.DELETED, runStartedAt);

        return RepeatStatus.FINISHED;
    }
}
