package com.example.springbatchinvestment.client.dto;

import com.example.springbatchinvestment.domain.FinancialGroupType;

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
        String finCoSubmDay,
        FinancialGroupType financialGroupType) {

    public FinancialProduct addFinancialGroupType(FinancialGroupType financialGroupType) {
        return new FinancialProduct(
                this.dclsMonth,
                this.finCoNo,
                this.finPrdtCd,
                this.korCoNm,
                this.finPrdtNm,
                this.joinWay,
                this.mtrtInt,
                this.spclCnd,
                this.joinDeny,
                this.joinMember,
                this.etcNote,
                this.maxLimit,
                this.dclsStrtDay,
                this.dclsEndDay,
                this.finCoSubmDay,
                financialGroupType);
    }
}
