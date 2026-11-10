package com.rayen.springds1.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OverdueCustomerResponse(
        Long customerId,
        String companyName,
        String email,
        Long overdueOrders,
        BigDecimal amountDue,
        LocalDateTime oldestOrderDate
) {
}
