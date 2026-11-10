package com.rayen.springds1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueReportResponse(LocalDate from, LocalDate to, Long paymentCount, BigDecimal totalRevenue) {
}
