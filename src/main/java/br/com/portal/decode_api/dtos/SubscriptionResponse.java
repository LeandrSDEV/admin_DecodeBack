package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.SubscriptionModule;
import br.com.portal.decode_api.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID decodeId,
        String decodeName,
        String decodeCode,
        String establishmentName,
        String clientName,
        SubscriptionModule module,
        String planName,
        BigDecimal price,
        BigDecimal discountPct,
        Integer durationDays,
        String features,
        SubscriptionStatus status,
        boolean active,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        String notes,
        LocalDateTime createdAt
) {}
