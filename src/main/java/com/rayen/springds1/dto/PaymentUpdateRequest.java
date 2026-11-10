package com.rayen.springds1.dto;

import com.rayen.springds1.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/** Only the method can change: the amount must always match the order total. */
public record PaymentUpdateRequest(@NotNull PaymentMethod method) {
}
