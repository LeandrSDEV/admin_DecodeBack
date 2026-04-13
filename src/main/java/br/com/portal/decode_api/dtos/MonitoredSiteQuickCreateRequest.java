package br.com.portal.decode_api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record MonitoredSiteQuickCreateRequest(
        @NotBlank @URL String url,
        @Size(max = 120) String name
) {}
