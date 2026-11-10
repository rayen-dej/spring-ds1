package com.rayen.springds1.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull Long customerId,
        @NotEmpty List<@NotNull @Valid OrderLineRequest> lines
) {
}
