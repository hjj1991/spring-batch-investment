package com.example.springbatchinvestment.client.dto;

import com.example.springbatchinvestment.domain.FinancialGroupType;

public record Company(
        String dclsMonth,
        String finCoNo,
        String korCoNm,
        String dclsChrgMan,
        String hompUrl,
        String calTel,
        FinancialGroupType financialGroupType) {

    public Company addFinancialGroupType(FinancialGroupType financialGroupType) {
        return new Company(
                this.dclsMonth,
                this.finCoNo,
                this.korCoNm,
                this.dclsChrgMan,
                this.hompUrl,
                this.calTel,
                financialGroupType);
    }
}
