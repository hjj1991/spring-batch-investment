package com.example.springbatchinvestment.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class DepositDto {

    private double intrRate2;
    private double intrRate;
    private String saveTrm;
    private String intrRateTypeNm;
    private String intrRateType;
    private String finPrdtCd;
    private String finCoNo;
    private String dclsMonth;

    private List<Option> options;

    @Getter
    @Setter
    public static class Result {
        private List<Option> optionList;
        private List<Baselist> baseList;
        @JsonProperty("err_msg")
        private String errMsg;
        @JsonProperty("err_cd")
        private String errCd;
        @JsonProperty("now_page_no")
        private String nowPageNo;
        @JsonProperty("max_page_no")
        private String maxPageNo;
        @JsonProperty("total_count")
        private String totalCount;
        @JsonProperty("prdt_div")
        private String prdtDiv;
    }

    @Getter
    @Setter
    public static class Option {
        @JsonProperty("intr_rate2")
        private double intrRate2;
        @JsonProperty("intr_rate")
        private double intrRate;
        @JsonProperty("save_trm")
        private String saveTrm;
        @JsonProperty("intr_rate_type_nm")
        private String intrRateTypeNm;
        @JsonProperty("intr_rate_type")
        private String intrRateType;
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("dcls_month")
        private String dclsMonth;
    }

    @Getter
    @Setter
    public static class Baselist {
        @JsonProperty("fin_co_subm_day")
        private String finCoSubmDay;
        @JsonProperty("dcls_strt_day")
        private String dclsStrtDay;
        @JsonProperty("max_limit")
        private long maxLimit;
        @JsonProperty("etc_note")
        private String etcNote;
        @JsonProperty("join_member")
        private String joinMember;
        @JsonProperty("join_deny")
        private String joinDeny;
        @JsonProperty("spcl_cnd")
        private String spclCnd;
        @JsonProperty("mtrt_int")
        private String mtrtInt;
        @JsonProperty("join_way")
        private String joinWay;
        @JsonProperty("fin_prdt_nm")
        private String finPrdtNm;
        @JsonProperty("kor_co_nm")
        private String korCoNm;
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("dcls_month")
        private String dclsMonth;
    }

    @Data
    public static class ResponseDepositApi {
        private BankDto.Result result;


        public boolean requestSuccess(){
            if(result != null && result.getErrCd().equals("000")){
                return true;
            }

            return false;
        }

        public boolean isOverLastPage(){
            if(requestSuccess() && Integer.parseInt(result.getMaxPageNo()) < Integer.parseInt(result.getNowPageNo())){
                return true;
            }

            return false;
        }
    }
}
