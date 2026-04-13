package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.InteractionChannel;
import br.com.portal.decode_api.enums.InteractionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteractionResponse(
        UUID id,
        String code,
        String contactName,
        InteractionChannel channel,
        String city,
        InteractionStatus status,
        UUID ownerUserId,
        String ownerName,
        UUID leadId,
        String leadCode,
        LocalDateTime lastMessageAt,
        LocalDateTime updatedAt
) {
}
