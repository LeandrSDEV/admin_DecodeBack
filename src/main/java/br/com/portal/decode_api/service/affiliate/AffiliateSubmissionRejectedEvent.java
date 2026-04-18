package br.com.portal.decode_api.service.affiliate;

import java.util.UUID;

/**
 * Evento publicado quando uma solicitacao de estabelecimento enviada por um
 * afiliado e rejeitada. Listener (ex.: WhatsApp) reage fora da transacao de
 * rejeicao.
 */
public record AffiliateSubmissionRejectedEvent(
        UUID submissionId,
        UUID affiliateId,
        String affiliateName,
        String affiliateWhatsapp,
        String establishmentName,
        String reason
) {}
