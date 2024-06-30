package com.example.springbatchinvestment.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public abstract class Result<T, U>{
    private final String errCd;
    private final String errMsg;
    private final Long totalCount;
    private final Long maxPageNo;
    private final Long nowPageNo;
    @JsonProperty("baseList")
    private final List<T> baseList;
    @JsonProperty("optionList")
    private final List<U> optionList;
}
