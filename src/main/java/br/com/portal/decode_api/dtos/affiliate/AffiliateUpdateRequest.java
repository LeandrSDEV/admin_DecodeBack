package br.com.portal.decode_api.dtos.affiliate;

import br.com.portal.decode_api.enums.AffiliateStatus;

import java.math.BigDecimal;

public record AffiliateUpdateRequest(
        String name,
        String whatsapp,
        String cpf,
        String city,
        String state,
        String pixKeyType,
        String pixKey,
        AffiliateStatus status,
        BigDecimal customCommissionRate,
        String notes
) {}
