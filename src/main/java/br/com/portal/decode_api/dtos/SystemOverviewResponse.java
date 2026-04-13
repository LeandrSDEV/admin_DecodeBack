package br.com.portal.decode_api.dtos;

import java.time.LocalDateTime;

public record SystemOverviewResponse(
        Double cpuLoad,
        Long memUsedBytes,
        Long memTotalBytes,
        Long diskUsedBytes,
        Long diskTotalBytes,
        Long uptimeSeconds,
        LocalDateTime sampledAt
) {}
