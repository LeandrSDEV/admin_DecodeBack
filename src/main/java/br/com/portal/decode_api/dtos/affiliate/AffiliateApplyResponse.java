package br.com.portal.decode_api.dtos.affiliate;

/** Resposta publica apos cadastro. Nao expoe dados sensiveis. */
public record AffiliateApplyResponse(
        String status,       // "PENDING"
        String message,      // mensagem amigavel pro form
        String refCode       // codigo ja gerado (mesmo antes de aprovar)
) {}
