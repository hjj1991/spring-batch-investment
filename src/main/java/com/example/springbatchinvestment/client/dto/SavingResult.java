package com.example.springbatchinvestment.client.dto;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class SavingResult extends Result<FinancialProduct, SavingOption> {
    public SavingResult(
            final String errCd,
            final String errMsg,
            final Long totalCount,
            final Long maxPageNo,
            final Long nowPageNo,
            final List<FinancialProduct> baseList,
            final List<SavingOption> optionList) {
        super(errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
