package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.AffiliateStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Representacao completa de um afiliado (admin view). */
public record AffiliateResponse(
        UUID id,
        String refCode,
        String name,
        String email,
        String whatsapp,
        String cpf,
        String city,
        String state,
        String pixKeyType,
        String pixKey,
        AffiliateStatus status,
        BigDecimal customCommissionRate,
        LocalDateTime lastLoginAt,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        long totalReferrals,
        long activeClients,
        BigDecimal totalEarned,
        BigDecimal pendingAmount
) {}
