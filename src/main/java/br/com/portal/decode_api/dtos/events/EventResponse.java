package br.com.portal.decode_api.dtos.events;

import java.time.LocalDateTime;
import java.util.Map;

public record EventResponse(
        long eventId,
        String type,
        LocalDateTime occurredAt,
        String leadId,
        String channel,
        String source,
        Map<String, Object> payload,
        String actorUserId,
        String traceId,
        String severity
) {}
