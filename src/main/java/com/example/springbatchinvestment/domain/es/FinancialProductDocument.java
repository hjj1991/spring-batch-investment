package com.example.springbatchinvestment.domain.es;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "financial_product", writeTypeHint = WriteTypeHint.FALSE, createIndex = false)
public class FinancialProductDocument {

    @Id private String id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String productName;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String companyName;

    @Field(type = FieldType.Keyword)
    private String financialProductCode;

    @Field(type = FieldType.Keyword)
    private String companyCode;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String specialCondition;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String joinWay;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String etcNote;

    @Field(type = FieldType.Keyword)
    private String financialGroupType;

    @Field(type = FieldType.Keyword)
    private String joinRestriction;

    @Field(type = FieldType.Keyword)
    private String financialProductType;

    @Field(type = FieldType.Nested)
    private List<Option> options;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] productVector;

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Option {
        @Field(type = FieldType.Keyword)
        private String interestRateType;

        @Field(type = FieldType.Keyword)
        private String reserveType;

        @Field(type = FieldType.Keyword)
        private String depositPeriodMonths;

        @Field(type = FieldType.Double)
        private Double initRate;

        @Field(type = FieldType.Double)
        private Double maxRate;
    }
}
