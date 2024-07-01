package com.example.springbatchinvestment.domain;

import lombok.Getter;

@Getter
public enum InterestRateType {
    SIMPLE("S", "단리"),
    COMPOUND("M", "복리");

    private final String code;
    private final String description;

    InterestRateType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InterestRateType fromCode(String code) {
        for (InterestRateType type : InterestRateType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
