package com.rayen.springds1.dto;

import java.math.BigDecimal;

/** Row returned by the revenue query. totalRevenue is null when no payment matches. */
public record RevenueTotals(Long paymentCount, BigDecimal totalRevenue) {
}
