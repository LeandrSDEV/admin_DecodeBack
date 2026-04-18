package br.com.portal.decode_api.service.affiliate;

import java.util.UUID;

/**
 * Evento publicado apos a aprovacao de um afiliado para que listeners
 * (ex.: notificacao WhatsApp) ajam fora da transacao do {@code approve}.
 * A senha inicial em texto claro e carregada aqui pois ela so existe no
 * momento da aprovacao (depois so temos o hash no banco).
 */
public record AffiliateApprovedEvent(
        UUID affiliateId,
        String name,
        String email,
        String whatsapp,
        String refCode,
        String initialPassword
) {}