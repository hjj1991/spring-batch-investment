package com.example.springbatchinvestment.client.dto;

import java.util.List;
import lombok.ToString;

@ToString(callSuper = true)
public class CompanyResult extends Result<Company, CompanyArea> {
    public CompanyResult(
            final String errCd,
            final String errMsg,
            final Long totalCount,
            final Long maxPageNo,
            final Long nowPageNo,
            final List<Company> baseList,
            final List<CompanyArea> optionList) {
        super(errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
