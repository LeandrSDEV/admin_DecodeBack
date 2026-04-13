package br.com.portal.decode_api.dtos.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record EventIngestRequest(
        @NotBlank @Size(max = 100) String type,
        LocalDateTime occurredAt,
        UUID leadId,
        @Size(max = 50) String channel,
        @Size(max = 50) String source,
        Map<String, Object> payload,
        @Size(max = 100) String traceId,
        @Size(max = 20) String severity
) {}
