package com.rayen.springds1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 120) String companyName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) @Pattern(regexp = "^[+0-9 ()-]{8,20}$") String phone,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 80) String city
) {
}
