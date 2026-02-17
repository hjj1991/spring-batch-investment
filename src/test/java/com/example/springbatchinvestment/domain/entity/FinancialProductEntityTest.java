package com.example.springbatchinvestment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.springbatchinvestment.domain.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinancialProductEntityTest {

    @Test
    void updateByProduct_옵션컬렉션은_가변리스트로_유지된다() {
        FinancialProductOptionEntity existingOption =
                FinancialProductOptionEntity.builder()
                        .dclsMonth("202602")
                        .interestRateType(InterestRateType.SIMPLE)
                        .depositPeriodMonths(12)
                        .baseInterestRate(BigDecimal.valueOf(1.2))
                        .maximumInterestRate(BigDecimal.valueOf(1.8))
                        .build();

        FinancialProductEntity entity =
                FinancialProductEntity.builder()
                        .financialProductCode("PRDT-001")
                        .financialProductName("기존상품")
                        .joinRestriction(JoinRestriction.NO_RESTRICTION)
                        .financialProductType(FinancialProductType.SAVINGS)
                        .joinMember("누구나")
                        .additionalNotes("비고")
                        .dclsMonth("202602")
                        .status(ProductStatus.ACTIVE)
                        .financialProductOptionEntities(new ArrayList<>(List.of(existingOption)))
                        .build();

        FinancialProductModel model =
                new FinancialProductModel(
                        "202602",
                        "0010001",
                        "PRDT-001",
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
                        List.of(
                                new FinancialProductOptionModel(
                                        "202602",
                                        "0010001",
                                        "PRDT-001",
                                        "S",
                                        "단리",
                                        null,
                                        null,
                                        "12",
                                        1.4,
                                        2.5)),
                        FinancialProductType.SAVINGS);

        entity.updateByProduct(model);

        assertThatCode(() -> entity.getFinancialProductOptionEntities().clear()).doesNotThrowAnyException();
        assertThat(entity.getFinancialProductOptionEntities()).isEmpty();
    }
}
