package com.example.springbatchinvestment.processor;

import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.es.FinancialProductDocument;
import com.example.springbatchinvestment.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialProductEsItemProcessor
        implements ItemProcessor<FinancialProductEntity, FinancialProductDocument> {

    private final EmbeddingService embeddingService;

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
                                                        .initRate(option.getBaseInterestRate().doubleValue())
                                                        .maxRate(option.getMaximumInterestRate().doubleValue())
                                                        .build())
                                .toList())
                .productVector(this.embeddingService.embed(this.createEmbeddingText(item)))
                .build();
    }

    @NotNull
    private String createEsId(FinancialProductEntity item) {
        return item.getFinancialCompanyEntity().getFinancialCompanyCode()
                + "-"
                + item.getFinancialProductCode();
    }

    private String createEmbeddingText(FinancialProductEntity item) {
        // 1. 데이터 정제
        String productName = item.getFinancialProductName().trim();
        String companyName = item.getFinancialCompanyEntity().getCompanyName().trim();
        String specialCondition =
                item.getSpecialCondition() != null
                        ? item.getSpecialCondition().replace("\n", " ").replaceAll("\\s+", " ").trim()
                        : "";
        String joinWay = item.getJoinWay() != null ? item.getJoinWay().trim() : "";
        String postMaturityInterestRate =
                item.getPostMaturityInterestRate() != null
                        ? item.getPostMaturityInterestRate().replace("\n", " ").replaceAll("\\s+", " ").trim()
                        : "";
        String etcNote =
                item.getAdditionalNotes() != null
                        ? item.getAdditionalNotes().replace("\n", " ").replaceAll("\\s+", " ").trim()
                        : "";

        // 2. 자연스러운 문장으로 조합
        return String.format(
                "%s에서 제공하는 '%s' 상품입니다. "
                        + "주요 우대조건은 '%s'이며, 가입은 주로 %s 방식으로 가능합니다. "
                        + "만기 후 이자율은 %s 입니다. "
                        + "기타 참고사항으로는 '%s' 내용이 있습니다.",
                companyName, productName, specialCondition, joinWay, postMaturityInterestRate, etcNote);
    }
}
