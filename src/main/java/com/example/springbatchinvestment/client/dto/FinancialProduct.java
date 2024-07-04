package com.example.springbatchinvestment.client.dto;

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
        String maxLimit,
        String dclsStrtDay,
        String dclsEndDay,
        String finCoSubmDay) {}
