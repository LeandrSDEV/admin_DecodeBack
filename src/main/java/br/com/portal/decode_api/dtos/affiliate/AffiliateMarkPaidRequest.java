package br.com.portal.decode_api.dtos.affiliate;

import jakarta.validation.constraints.NotBlank;

public record AffiliateMarkPaidRequest(
        @NotBlank String paidReference,   // comprovante ou EndToEndId do PIX
        String notes
) {}
