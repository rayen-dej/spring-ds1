package com.rayen.springds1.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLineUpdateRequest(@NotNull @Min(1) Integer quantity) {
}
