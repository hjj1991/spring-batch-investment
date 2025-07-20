package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.*;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import com.example.springbatchinvestment.service.embedding.EmbeddingService;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialProductItemWriter implements ItemWriter<FinancialProductModel> {

    private final FinancialProductRepository financialProductRepository;
    private final FinancialCompanyRepository financialCompanyRepository;
    private final EmbeddingService embeddingService;

    @Override
    public void write(Chunk<? extends FinancialProductModel> chunk) throws Exception {
        chunk
                .getItems()
                .forEach(
                        financialProductModel -> {
                            Optional<FinancialProductEntity> optionalFinancialProductEntity =
                                    this.financialProductRepository
                                            .findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCode(
                                                    financialProductModel.finCoNo(), financialProductModel.finPrdtCd());
                            FinancialProductEntity financialProductEntity;
                            String newContentHash = financialProductModel.generateContentHash();
                            String embeddingText = this.createEmbeddingText(financialProductModel);

                            if (optionalFinancialProductEntity.isPresent()) {
                                financialProductEntity = optionalFinancialProductEntity.get();
                                financialProductEntity.updateByProduct(financialProductModel);

                                if (!newContentHash.equals(financialProductEntity.getProductContentHash())) {
                                    float[] embeddingVector = this.embeddingService.embed(embeddingText);
                                    financialProductEntity.updateProductContentHash(newContentHash);
                                    financialProductEntity.updateEmbeddingVector(embeddingVector);
                                }
                            } else {
                                float[] embeddingVector = this.embeddingService.embed(embeddingText);
                                financialProductEntity =
                                        FinancialProductEntity.builder()
                                                .financialCompanyEntity(
                                                        this.financialCompanyRepository
                                                                .findByFinancialCompanyCode(financialProductModel.finCoNo())
                                                                .orElseThrow(
                                                                        () ->
                                                                                new IllegalArgumentException(
                                                                                        "financialCompanyEntity not found")))
                                                .financialProductCode(financialProductModel.finPrdtCd())
                                                .financialProductName(financialProductModel.finPrdtNm())
                                                .joinWay(financialProductModel.joinWay())
                                                .postMaturityInterestRate(financialProductModel.mtrtInt())
                                                .specialCondition(financialProductModel.spclCnd())
                                                .joinRestriction(
                                                        JoinRestriction.fromCode(
                                                                Integer.parseInt(financialProductModel.joinDeny())))
                                                .financialProductType(financialProductModel.financialProductType())
                                                .joinMember(financialProductModel.joinMember())
                                                .additionalNotes(financialProductModel.etcNote())
                                                .maxLimit(
                                                        Optional.ofNullable(financialProductModel.maxLimit())
                                                                .map(Long::valueOf)
                                                                .orElse(null))
                                                .dclsStartDay(financialProductModel.dclsStrtDay())
                                                .dclsEndDay(financialProductModel.dclsEndDay())
                                                .financialSubmitDay(financialProductModel.finCoSubmDay())
                                                .status(ProductStatus.ACTIVE)
                                                .productContentHash(newContentHash)
                                                .embeddingVector(embeddingVector)
                                                .build();

                                financialProductEntity
                                        .getFinancialProductOptionEntities()
                                        .addAll(
                                                financialProductModel.financialProductOptionModels().stream()
                                                        .map(
                                                                financialProductOptionModel ->
                                                                        FinancialProductOptionEntity.builder()
                                                                                .financialProductEntity(financialProductEntity)
                                                                                .interestRateType(
                                                                                        InterestRateType.fromCode(
                                                                                                financialProductOptionModel.intrRateType()))
                                                                                .reserveType(
                                                                                        ReserveType.fromCode(
                                                                                                financialProductOptionModel.rsrvType()))
                                                                                .depositPeriodMonths(financialProductOptionModel.saveTrm())
                                                                                .baseInterestRate(
                                                                                        Optional.ofNullable(
                                                                                                        financialProductOptionModel.intrRate())
                                                                                                .map(BigDecimal::valueOf)
                                                                                                .orElse(null))
                                                                                .maximumInterestRate(
                                                                                        Optional.ofNullable(
                                                                                                        financialProductOptionModel.intrRate2())
                                                                                                .map(BigDecimal::valueOf)
                                                                                                .orElse(null))
                                                                                .build())
                                                        .toList());
                            }
                            this.financialProductRepository.save(financialProductEntity);
                        });
    }

    private String createEmbeddingText(FinancialProductModel item) {
        // 1. 데이터 정제
        String productName = item.finPrdtNm().trim();
        String companyName = item.korCoNm().trim();
        String specialCondition =
                item.spclCnd() != null
                        ? item.spclCnd().replace("\n", " ").replaceAll("\\s+", " ").trim()
                        : "";
        String joinWay = item.joinWay() != null ? item.joinWay().trim() : "";
        String postMaturityInterestRate =
                item.mtrtInt() != null
                        ? item.mtrtInt().replace("\n", " ").replaceAll("\\s+", " ").trim()
                        : "";
        String etcNote =
                item.etcNote() != null
                        ? item.etcNote().replace("\n", " ").replaceAll("\\s+", " ").trim()
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
