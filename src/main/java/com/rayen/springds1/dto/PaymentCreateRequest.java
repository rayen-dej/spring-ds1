package com.rayen.springds1.dto;

import java.math.BigDecimal;

import com.rayen.springds1.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record PaymentCreateRequest(
        @NotNull Long orderId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod method
) {
}
