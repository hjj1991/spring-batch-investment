package com.example.springbatchinvestment.client.dto;

import java.util.Optional;

public record FinancialProduct(
        String dclsMonth,
        String finCoNo,
        String finPrdtCd,
        String korCoNm,
        String finPrdtNm,
        String joinWay,
        String mtrtInt,
        String spclCnd,
        String joinDeny,
        String joinMember,
        String etcNote,
        Optional<String> maxLimit,
        String dclsStrtDay,
        Optional<String> dclsEndDay,
        String finCoSubmDay
) {
}
