package com.example.springbatchinvestment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinancialGroupType {
    BANK("020000", "은행"),
    SAVING_BANK("030300", "저축은행");

    private final String financialGroupCode;
    private final String title;

    public static FinancialGroupType fromCode(String code) {
        for (FinancialGroupType type : FinancialGroupType.values()) {
            if (type.financialGroupCode.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown financial group code: " + code);
    }

    public static FinancialGroupType fromTitle(String title) {
        for (FinancialGroupType type : FinancialGroupType.values()) {
            if (type.title.equalsIgnoreCase(title)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown financial group title: " + title);
    }
}
