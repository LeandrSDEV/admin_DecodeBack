package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.DecodeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DecodeResponse(
        UUID id,
        String code,
        String name,
        String city,
        DecodeStatus status,
        Integer usersCount,
        BigDecimal monthlyRevenue,
        UUID affiliateId,
        String affiliateName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
