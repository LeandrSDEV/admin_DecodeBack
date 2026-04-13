package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.AffiliateStatus;

import java.util.UUID;

public record AffiliateLoginResponse(
        String token,
        long expiresInSeconds,
        UUID affiliateId,
        String name,
        String email,
        String refCode,
        AffiliateStatus status,
        boolean mustChangePassword
) {}
