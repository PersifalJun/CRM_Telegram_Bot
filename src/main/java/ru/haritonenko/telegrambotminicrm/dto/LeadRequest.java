package ru.haritonenko.telegrambotminicrm.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LeadRequest(
        @NotBlank String fio,
        @NotBlank String phone,
        @NotBlank String district,
        @NotBlank String source,
        @NotNull @Min(0) Integer quantity,
        @NotNull @DecimalMin("0.0") @Digits(integer = 12, fraction = 2) BigDecimal amount
) {
}
