package com.rayen.springds1.dto;

import com.rayen.springds1.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}
