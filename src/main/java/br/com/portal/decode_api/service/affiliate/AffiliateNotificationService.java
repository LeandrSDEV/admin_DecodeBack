package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.service.whatsapp.WhatsAppBridgeClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ouve {@link AffiliateApprovedEvent} e envia a mensagem de boas vindas com
 * as credenciais de acesso ao portal do afiliado via whatsapp-bridge.
 * <p>
 * Roda apos o commit da transacao de aprovacao ({@code AFTER_COMMIT}) e de
 * forma assincrona — falha no WhatsApp nao afeta a aprovacao do afiliado.
 */
@Service
@RequiredArgsConstructor
public class AffiliateNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateNotificationService.class);

    private final WhatsAppBridgeClient bridgeClient;
    private final AffiliateProperties affiliateProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAffiliateApproved(AffiliateApprovedEvent event) {
        if (event.whatsapp() == null || event.whatsapp().isBlank()) {
            log.warn("Afiliado {} aprovado sem WhatsApp cadastrado; notificacao pulada", event.affiliateId());
            return;
        }
        if (!bridgeClient.isEnabled()) {
            log.warn("whatsapp-bridge nao configurado; notificacao do afiliado {} nao sera enviada", event.affiliateId());
            return;
        }
        try {
            String message = buildWelcomeMessage(event);
            bridgeClient.sendText(event.whatsapp(), message);
            log.info("Mensagem de boas vindas enviada ao afiliado {} ({})", event.affiliateId(), event.email());
        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp de boas vindas para afiliado {}: {}",
                    event.affiliateId(), e.getMessage(), e);
        }
    }

    private String buildWelcomeMessage(AffiliateApprovedEvent event) {
        String portalUrl = affiliateProperties.getPortalBaseUrl();
        String firstName = extractFirstName(event.name());

        return """
                *Parabens, %s!* 🎉

                Seu cadastro no Programa de Afiliados Decode foi *aprovado*.

                A partir de agora voce pode acompanhar comissoes, fechamentos, materiais de venda e indicacoes direto no portal.

                *Seus dados de acesso*
                - Portal: %s
                - Login (email): %s
                - Senha inicial: %s
                - Codigo de indicacao (refCode): %s

                Por seguranca, no primeiro login voce sera solicitado a *trocar a senha*.

                *O que voce encontra no portal*
                - Dashboard com indicacoes ativas e estimativa do mes
                - Historico de comissoes (pendentes, carencia, pagas)
                - Calendario de pagamentos (payout runs)
                - Manuais e materiais de divulgacao
                - Seu link com tracking: %s?ref=%s

                Qualquer duvida, responda esta mensagem. Boas vendas! 🚀
                """.formatted(
                firstName,
                portalUrl,
                event.email(),
                event.initialPassword(),
                event.refCode(),
                affiliateProperties.getLandingBaseUrl(),
                event.refCode()
        );
    }

    private String extractFirstName(String fullName) {
        if (fullName == null) return "Afiliado";
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) return "Afiliado";
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}