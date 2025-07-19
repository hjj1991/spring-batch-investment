package com.example.springbatchinvestment.client.dto;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class CompanyResult extends Result<Company, CompanyArea> {
    public CompanyResult(
            final String prdtDiv,
            final String errCd,
            final String errMsg,
            final Long totalCount,
            final Long maxPageNo,
            final Long nowPageNo,
            final List<Company> baseList,
            final List<CompanyArea> optionList) {
        super(prdtDiv, errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
