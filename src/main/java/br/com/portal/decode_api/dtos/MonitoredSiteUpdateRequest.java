package br.com.portal.decode_api.dtos;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record MonitoredSiteUpdateRequest(
        @Size(min = 2, max = 120) String name,
        @URL String url,
        Boolean enabled,
        @Positive Integer slowThresholdMs,
        @Positive Integer timeoutMs
) {
}
