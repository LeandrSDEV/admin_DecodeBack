package br.com.portal.decode_api.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionRequest(
        UUID decodeId,
        @Size(max = 160) String establishmentName,
        @Size(max = 160) String clientName,
        @NotBlank @Size(min = 2, max = 120) String planName,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero BigDecimal discountPct,
        @NotNull @Positive Integer durationDays,
        String features,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        String notes
) {}
