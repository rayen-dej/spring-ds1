package com.rayen.springds1.dto;

import jakarta.validation.constraints.NotNull;

/** Lines are managed through /api/orders/{id}/lines and status through PATCH /api/orders/{id}/status. */
public record OrderUpdateRequest(@NotNull Long customerId) {
}
