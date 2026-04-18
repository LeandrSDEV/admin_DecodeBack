package br.com.portal.decode_api.dtos.affiliate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AffiliateDecodeSubmissionRejectRequest(
        @NotBlank @Size(min = 3, max = 500) String reason
) {}
