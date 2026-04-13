package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.InteractionChannel;
import br.com.portal.decode_api.enums.InteractionStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InteractionUpdateRequest(
        @Size(min = 2, max = 150) String contactName,
        InteractionChannel channel,
        @Size(min = 2, max = 120) String city,
        InteractionStatus status,
        UUID ownerUserId,
        UUID leadId
) {
}
