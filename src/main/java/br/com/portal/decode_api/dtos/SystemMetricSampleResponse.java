package br.com.portal.decode_api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemMetricSampleResponse(
        UUID id,
        LocalDateTime sampledAt,
        double cpuLoad,
        long memUsedBytes,
        long memTotalBytes,
        long diskUsedBytes,
        long diskTotalBytes,
        long uptimeSeconds
) {}