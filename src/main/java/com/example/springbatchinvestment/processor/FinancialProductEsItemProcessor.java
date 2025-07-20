package com.example.springbatchinvestment.processor;

import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.es.FinancialProductDocument;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialProductEsItemProcessor
        implements ItemProcessor<FinancialProductEntity, FinancialProductDocument> {

    @Override
    public FinancialProductDocument process(@NotNull FinancialProductEntity item) throws Exception {
        // Convert enum to string
        return FinancialProductDocument.builder()
                .id(this.createEsId(item))
                .productName(item.getFinancialProductName())
                .companyName(item.getFinancialCompanyEntity().getCompanyName())
                .financialProductCode(item.getFinancialProductCode())
                .companyCode(item.getFinancialCompanyEntity().getFinancialCompanyCode())
                .specialCondition(item.getSpecialCondition())
                .joinWay(item.getJoinWay())
                .etcNote(item.getAdditionalNotes())
                .status(item.getStatus().name()) // Convert enum to string
                .options(
                        item.getFinancialProductOptionEntities().stream()
                                .map(
                                        option ->
                                                FinancialProductDocument.Option.builder()
                                                        .interestRateType(option.getInterestRateType().name())
                                                        .reserveType(
                                                                option.getReserveType() != null
                                                                        ? option.getReserveType().name()
                                                                        : null)
                                                        .saveTerm(Integer.parseInt(option.getDepositPeriodMonths()))
                                                        .initRate(
                                                                option.getBaseInterestRate() != null
                                                                        ? option.getBaseInterestRate().doubleValue()
                                                                        : 0.0)
                                                        .maxRate(
                                                                option.getMaximumInterestRate() != null
                                                                        ? option.getMaximumInterestRate().doubleValue()
                                                                        : 0.0)
                                                        .build())
                                .toList())
                .productVector(item.getEmbeddingVector())
                .build();
    }

    @NotNull
    private String createEsId(FinancialProductEntity item) {
        return item.getFinancialCompanyEntity().getFinancialCompanyCode()
                + "-"
                + item.getFinancialProductCode();
    }
}
