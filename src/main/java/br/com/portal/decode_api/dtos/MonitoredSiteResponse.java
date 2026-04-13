package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.SiteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonitoredSiteResponse(
        UUID id,
        String code,
        String name,
        String url,
        Boolean enabled,
        SiteStatus status,
        Double uptime30d,
        LocalDateTime lastCheckAt,
        Integer rttMs,
        Integer slowThresholdMs,
        Integer timeoutMs
) {
}
