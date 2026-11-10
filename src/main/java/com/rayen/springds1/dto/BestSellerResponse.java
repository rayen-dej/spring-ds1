package com.rayen.springds1.dto;

import java.math.BigDecimal;

public record BestSellerResponse(Long productId, String sku, String name, Long unitsSold, BigDecimal revenue) {
}
