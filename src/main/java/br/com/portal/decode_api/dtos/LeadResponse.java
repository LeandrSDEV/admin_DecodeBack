package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.LeadSource;
import br.com.portal.decode_api.enums.LeadStage;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String code,
        String name,
        String phone,
        String email,
        String status,
        Integer score,
        LocalDateTime lastContactAt,
        LeadSource source,
        LeadStage stage,
        UUID ownerUserId,
        String ownerName,
        LocalDateTime updatedAt
) {
}
