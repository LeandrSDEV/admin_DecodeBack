package br.com.portal.decode_api.dtos;

import java.time.LocalDateTime;

public record DashboardSummaryResponse(
        String applicationStatus,
        String monitoredSiteStatus,
        Integer incidentsOpen,
        LocalDateTime lastCheckAt,
        String message
) {
}