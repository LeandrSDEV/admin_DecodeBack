package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.AffiliateCommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AffiliateCommissionResponse(
        UUID id,
        UUID affiliateId,
        String affiliateName,
        UUID decodeId,
        String decodeName,
        LocalDate referenceMonth,
        String planName,
        BigDecimal planPrice,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        AffiliateCommissionStatus status,
        LocalDate carenciaUntil,
        UUID payoutRunId,
        LocalDateTime paidAt,
        String paidReference,
        String notes
) {}
