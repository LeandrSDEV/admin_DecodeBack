package br.com.portal.decode_api.dtos.affiliate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AffiliateApproveRequest(
        @NotBlank @Size(min = 8, max = 120) String initialPassword,
        String notes
) {}
