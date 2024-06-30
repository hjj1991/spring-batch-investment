package com.example.springbatchinvestment.domain;

import lombok.Getter;

@Getter
public enum JoinRestriction {
    NO_RESTRICTION(1, "제한없음"),
    LOW_INCOME_ONLY(2, "서민전용"),
    PARTIALLY_RESTRICTED(3, "일부제한");

    private final int code;
    private final String description;

    JoinRestriction(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String toString() {
        return this.code + ": " + this.description;
    }
}