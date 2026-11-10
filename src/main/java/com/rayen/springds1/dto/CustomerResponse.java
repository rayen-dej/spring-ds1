package com.rayen.springds1.dto;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String companyName,
        String email,
        String phone,
        String address,
        String city,
        LocalDateTime createdAt
) {
}
