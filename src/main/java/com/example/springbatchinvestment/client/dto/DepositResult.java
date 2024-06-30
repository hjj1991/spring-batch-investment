package com.example.springbatchinvestment.client.dto;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = false)
@Value
public class DepositResult extends Result<FinancialProduct, DepositOption>{
    public DepositResult(final String errCd, final String errMsg, final Long totalCount, final Optional<Long> maxPageNo, final Optional<Long> nowPageNo, final List<FinancialProduct> baseList, final List<DepositOption> optionList) {
        super(errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
