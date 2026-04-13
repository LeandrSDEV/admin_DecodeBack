package br.com.portal.decode_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record MonitoredSiteCreateRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @URL String url,
        Boolean enabled,
        @Positive Integer slowThresholdMs,
        @Positive Integer timeoutMs
) {
}
