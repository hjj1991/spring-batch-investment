package com.example.springbatchinvestment.client.dto;

import java.util.Objects;

public record FssResponse<T extends Result>(
    T result
) {

    public boolean isSuccess() {
        return Objects.equals(this.result.getErrCd(), "000");
    }
}
