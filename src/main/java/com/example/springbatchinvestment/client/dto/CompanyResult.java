package com.example.springbatchinvestment.client.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class CompanyResult extends Result<Company, CompanyArea> {
    @JsonCreator
    public CompanyResult(
            @JsonProperty("prdt_div") final String prdtDiv,
            @JsonProperty("err_cd") final String errCd,
            @JsonProperty("err_msg") final String errMsg,
            @JsonProperty("total_count") final Long totalCount,
            @JsonProperty("max_page_no") final Long maxPageNo,
            @JsonProperty("now_page_no") final Long nowPageNo,
            @JsonProperty("baseList") final List<Company> baseList,
            @JsonProperty("optionList") final List<CompanyArea> optionList) {
        super(prdtDiv, errCd, errMsg, totalCount, maxPageNo, nowPageNo, baseList, optionList);
    }
}
