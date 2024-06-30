package com.example.springbatchinvestment.client.dto;

public record DepositOption(
        String dclsMonth,
        String finCoNo,
        String finPrdtCd,
        String intrRateType,
        String intrRateTypeNm,
        String saveTrm,
        Double intrRate,
        Double intrRate2
) {
}
