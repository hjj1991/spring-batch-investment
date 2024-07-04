package com.example.springbatchinvestment.domain;

import com.example.springbatchinvestment.client.dto.FinancialProductOption;

public record FinancialProductOptionModel(
        String dclsMonth,
        String finCoNo,
        String finPrdtCd,
        String intrRateType,
        String intrRateTypeNm,
        String rsrvType,
        String rsrvTypeNm,
        String saveTrm,
        Double intrRate,
        Double intrRate2) {

    public static FinancialProductOptionModel from(FinancialProductOption financialProductOption) {
        return new FinancialProductOptionModel(
                financialProductOption.dclsMonth(),
                financialProductOption.finCoNo(),
                financialProductOption.finPrdtCd(),
                financialProductOption.intrRateType(),
                financialProductOption.intrRateTypeNm(),
                financialProductOption.rsrvType(),
                financialProductOption.rsrvTypeNm(),
                financialProductOption.saveTrm(),
                financialProductOption.intrRate(),
                financialProductOption.intrRate2());
    }
}
