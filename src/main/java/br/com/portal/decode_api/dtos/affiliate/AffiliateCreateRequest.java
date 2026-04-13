package br.com.portal.decode_api.dtos.affiliate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Criacao manual de afiliado pelo admin (ja aprovado). */
public record AffiliateCreateRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 30) String whatsapp,
        @Size(max = 14) String cpf,
        @Size(max = 120) String city,
        @Size(max = 4) String state,
        @Size(max = 20) String pixKeyType,
        @Size(max = 180) String pixKey,
        String refCode,               // opcional - senao gera automatico
        BigDecimal customCommissionRate,
        String initialPassword,       // se nulo, envia email com link
        UUID decodeId                 // opcional - vincula este decode ao afiliado na criacao
) {}
