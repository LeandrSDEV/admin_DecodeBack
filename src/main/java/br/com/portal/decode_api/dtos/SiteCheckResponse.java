package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.SiteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiteCheckResponse(
        UUID id,
        LocalDateTime checkedAt,
        SiteStatus status,
        Integer rttMs,
        Integer httpStatus,
        Integer dnsMs,
        Integer tlsMs,
        Integer ttfbMs,
        Long bytesRead,
        String resolvedIp,
        String finalUrl,
        Integer redirectsCount,
        LocalDateTime sslValidTo,
        Integer sslDaysLeft,
        String metricsEndpoint,
        String metricsJson,
        String errorMessage
) {}
