package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.MessageDirection;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteractionMessageResponse(
        UUID id,
        UUID interactionId,
        MessageDirection direction,
        String body,
        LocalDateTime sentAt
) {
}
