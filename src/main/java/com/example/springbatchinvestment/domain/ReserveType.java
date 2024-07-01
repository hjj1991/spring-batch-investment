package com.example.springbatchinvestment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReserveType {
    FLEXIBLE("F", "자유적립식"),
    FIXED("S", "정액적립식");

    private final String code;
    private final String description;

    ReserveType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ReserveType fromCode(String code) {
        for (ReserveType type : ReserveType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
