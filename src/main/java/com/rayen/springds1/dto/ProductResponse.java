package com.rayen.springds1.dto;

import java.math.BigDecimal;
import java.util.Set;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal unitPrice,
        Integer stockQuantity,
        Integer reorderLevel,
        boolean active,
        Long categoryId,
        String categoryName,
        Set<SupplierResponse> suppliers
) {
}
