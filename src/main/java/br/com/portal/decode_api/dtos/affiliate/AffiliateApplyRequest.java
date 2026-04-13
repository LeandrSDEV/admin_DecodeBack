package br.com.portal.decode_api.dtos.affiliate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulario publico de inscricao de afiliado (landing no Lovable). */
public record AffiliateApplyRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 30) String whatsapp,
        @Size(max = 14) String cpf,
        @Size(max = 120) String city,
        @Size(max = 4) String state,
        @Size(max = 20) String pixKeyType,
        @Size(max = 180) String pixKey,
        @NotBlank String termsVersion
) {}
