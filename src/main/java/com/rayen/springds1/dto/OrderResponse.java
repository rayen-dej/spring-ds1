package com.rayen.springds1.dto;

import com.rayen.springds1.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        LocalDateTime orderDate,
        OrderStatus status,
        BigDecimal totalAmount,
        Long customerId,
        String customerCompanyName,
        List<OrderLineResponse> lines,
        PaymentResponse payment
) {
}
