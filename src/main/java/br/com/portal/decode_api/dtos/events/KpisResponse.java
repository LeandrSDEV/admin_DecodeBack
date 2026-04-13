package br.com.portal.decode_api.dtos.events;

import java.util.List;
import java.util.Map;

public record KpisResponse(
        String window,
        double eventsPerSecond,
        long totalEvents,
        long messagesIn,
        long messagesOut,
        long leadsNew,
        long failures,
        Double latencyAvgMs,
        Double latencyP95Ms,
        List<Map<String, Object>> epsSeries,
        List<Map<String, Object>> byChannel
) {}
