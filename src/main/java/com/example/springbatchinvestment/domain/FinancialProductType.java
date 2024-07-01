package com.example.springbatchinvestment.domain;

import lombok.Getter;

@Getter
public enum FinancialProductType {
    SAVINGS("예금"),
    INSTALLMENT_SAVINGS("적금");

    private final String description;

    FinancialProductType(String description) {
        this.description = description;
    }
}
