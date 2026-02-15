package com.example.springbatchinvestment.client.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class FinancialProductResult extends Result<FinancialProduct, FinancialProductOption> {
    @JsonCreator
    public FinancialProductResult(
            @JsonProperty("prdt_div") final String prdtDiv,
            @JsonProperty("err_cd") final String errCd,
            @JsonProperty("err_msg") final String errMsg,
            @JsonProperty("total_count") final Long totalCount,
            @JsonProperty("max_page_no") final Long maxPageNo,
            @JsonProperty("now_page_no") final Long nowPageNo,
            @JsonProperty("baseList") final List<FinancialProduct> baseList,
            @JsonProperty("optionList") final List<FinancialProductOption> optionList) {
        super(prdtDiv, errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
