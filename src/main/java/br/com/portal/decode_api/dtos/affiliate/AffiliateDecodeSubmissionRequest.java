package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.SubscriptionModule;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Payload enviado pelo afiliado quando submete uma nova proposta de
 * estabelecimento fechado. O admin depois revisa e aprova/rejeita.
 */
public record AffiliateDecodeSubmissionRequest(
        @NotBlank @Size(min = 2, max = 180) String establishmentName,
        @NotBlank @Size(min = 2, max = 120) String city,
        @Size(max = 4) String state,
        @Size(max = 20) String cnpj,

        @NotBlank @Size(min = 2, max = 180) String contactName,
        @Email @Size(max = 180) String contactEmail,
        @NotBlank @Size(min = 8, max = 30) String contactPhone,

        @PositiveOrZero Integer estimatedUsersCount,
        @PositiveOrZero BigDecimal estimatedMonthlyRevenue,

        @NotNull SubscriptionModule planModule,
        @NotBlank @Size(min = 2, max = 120) String planName,
        @NotNull @Positive BigDecimal planPrice,
        @PositiveOrZero BigDecimal planDiscountPct,
        @NotNull @Positive Integer planDurationDays,
        String planFeatures,

        String notes
) {}
