package com.example.springbatchinvestment.client.dto;

import com.example.springbatchinvestment.domain.FinancialGroupType;

public record Company(
        String dclsMonth,
        String finCoNo,
        String korCoNm,
        String dclsChrgMan,
        String hompUrl,
        String calTel,
        FinancialGroupType financialGroupType) {}
