package com.example.springbatchinvestment.client.dto;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = false)
@Value
public class SavingResult extends Result<FinancialProduct, SavingOption>{
    public SavingResult(final String errCd, final String errMsg, final Long totalCount, final Optional<Long> maxPageNo, final Optional<Long> nowPageNo, final List<FinancialProduct> baseList, final List<SavingOption> optionList) {
        super(errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
