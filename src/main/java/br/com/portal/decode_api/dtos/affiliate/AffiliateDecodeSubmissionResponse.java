package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.AffiliateDecodeSubmissionStatus;
import br.com.portal.decode_api.enums.SubscriptionModule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AffiliateDecodeSubmissionResponse(
        UUID id,
        UUID affiliateId,
        String affiliateName,
        String affiliateRefCode,

        String establishmentName,
        String city,
        String state,
        String cnpj,

        String contactName,
        String contactEmail,
        String contactPhone,

        Integer estimatedUsersCount,
        BigDecimal estimatedMonthlyRevenue,

        SubscriptionModule planModule,
        String planName,
        BigDecimal planPrice,
        BigDecimal planDiscountPct,
        Integer planDurationDays,
        String planFeatures,

        String notes,

        AffiliateDecodeSubmissionStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewedByName,
        String rejectionReason,

        UUID decodeId,
        String decodeCode,
        UUID subscriptionId,

        LocalDateTime createdAt
) {}
