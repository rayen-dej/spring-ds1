package com.rayen.springds1.dto;

import com.rayen.springds1.model.enums.PaymentMethod;
import com.rayen.springds1.model.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime paidAt,
        Long orderId
) {
}
