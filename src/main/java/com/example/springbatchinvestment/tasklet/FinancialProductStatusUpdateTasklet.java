package com.example.springbatchinvestment.tasklet;

import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
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

        // Set all existing SAVINGS products to DELETED
        this.financialProductRepository.updateStatusByFinancialProductType(
                FinancialProductType.SAVINGS, ProductStatus.DELETED);

        // Set all existing INSTALLMENT_SAVINGS products to DELETED
        this.financialProductRepository.updateStatusByFinancialProductType(
                FinancialProductType.INSTALLMENT_SAVINGS, ProductStatus.DELETED);

        return RepeatStatus.FINISHED;
    }
}
