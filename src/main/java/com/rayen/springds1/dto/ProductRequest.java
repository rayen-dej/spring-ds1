package com.rayen.springds1.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "must contain only letters, digits and hyphens") String sku,
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal unitPrice,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull @Min(1) Integer reorderLevel,
        Boolean active,
        @NotNull Long categoryId,
        Set<@NotNull Long> supplierIds
) {
}
