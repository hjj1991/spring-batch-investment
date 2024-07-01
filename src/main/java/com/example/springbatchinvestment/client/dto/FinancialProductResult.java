package com.example.springbatchinvestment.client.dto;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class FinancialProductResult extends Result<FinancialProduct, FinancialProductOption> {
    public FinancialProductResult(
            final String errCd,
            final String errMsg,
            final Long totalCount,
            final Long maxPageNo,
            final Long nowPageNo,
            final List<FinancialProduct> baseList,
            final List<FinancialProductOption> optionList) {
        super(errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
