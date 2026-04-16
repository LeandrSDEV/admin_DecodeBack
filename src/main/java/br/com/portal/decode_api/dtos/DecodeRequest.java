package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.DecodeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DecodeRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Size(min = 2, max = 120) String city,
        DecodeStatus status,
        @PositiveOrZero Integer usersCount,
        @PositiveOrZero BigDecimal monthlyRevenue,
        UUID affiliateId,
        Long tenantId
) {
}
