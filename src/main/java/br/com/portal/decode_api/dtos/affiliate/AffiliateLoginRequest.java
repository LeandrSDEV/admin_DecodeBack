package br.com.portal.decode_api.dtos.affiliate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AffiliateLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
