package br.com.portal.decode_api.dtos;

import java.time.LocalDateTime;

public record MonitoringSummaryResponse(
        int okCount,
        int slowCount,
        int downCount,
        long incidentsOpen,
        LocalDateTime lastCheckAt
) {
}
