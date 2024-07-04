package com.example.springbatchinvestment.writer;

import com.example.springbatchinvestment.domain.*;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import com.example.springbatchinvestment.repository.FinancialCompanyRepository;
import com.example.springbatchinvestment.repository.FinancialProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public class FinancialProductItemWriter implements ItemWriter<FinancialProductModel> {

    private final FinancialProductRepository financialProductRepository;
    private final FinancialCompanyRepository financialCompanyRepository;

    public FinancialProductItemWriter(
            FinancialProductRepository financialProductRepository,
            FinancialCompanyRepository financialCompanyRepository) {
        this.financialProductRepository = financialProductRepository;
        this.financialCompanyRepository = financialCompanyRepository;
    }

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
                            if (optionalFinancialProductEntity.isPresent()) {
                                financialProductEntity = optionalFinancialProductEntity.get();
                                financialProductEntity.updateByProduct(financialProductModel);

                            } else {
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
}
