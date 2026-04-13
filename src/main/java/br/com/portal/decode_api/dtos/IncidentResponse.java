package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.IncidentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        IncidentStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String message,
        UUID lastCheckId
) {}
