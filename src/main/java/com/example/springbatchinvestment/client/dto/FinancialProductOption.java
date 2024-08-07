package com.example.springbatchinvestment.client.dto;

public record FinancialProductOption(
        String dclsMonth,
        String finCoNo,
        String finPrdtCd,
        String intrRateType,
        String intrRateTypeNm,
        String rsrvType,
        String rsrvTypeNm,
        String saveTrm,
        Double intrRate,
        Double intrRate2) {}
