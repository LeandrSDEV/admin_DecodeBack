package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.AffiliatePayoutRunStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AffiliatePayoutRunResponse(
        UUID id,
        LocalDate referenceMonth,
        int totalAffiliates,
        int totalCommissions,
        BigDecimal totalAmount,
        AffiliatePayoutRunStatus status,
        LocalDateTime generatedAt,
        LocalDateTime reviewedAt,
        LocalDateTime completedAt,
        String notes
) {}
