package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.MessageDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record InteractionMessageCreateRequest(
        MessageDirection direction,
        @NotBlank @Size(max = 5000) String body,
        LocalDateTime sentAt
) {
}
