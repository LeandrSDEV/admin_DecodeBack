package br.com.portal.decode_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracao do cliente HTTP para o whatsapp-bridge (Baileys) rodando dentro
 * da rede do restaurant. O Admin_Decode consome o mesmo bridge para enviar
 * notificacoes (ex.: credenciais de acesso do afiliado aprovado).
 */
@Component
@ConfigurationProperties(prefix = "app.whatsapp-bridge")
@Getter
@Setter
public class WhatsAppBridgeProperties {

    /** Liga/desliga o envio. Quando false, o servico apenas logga. */
    private boolean enabled = true;

    /** URL do bridge (ex.: http://whatsapp-bridge:21465 ou http://host:21465). */
    private String baseUrl = "http://whatsapp-bridge:21465";

    /** Token compartilhado com o bridge (header x-internal-token). */
    private String token = "";

    /** Nome da instancia configurada no bridge. */
    private String instance = "lanchonete";

    /** Timeouts. */
    private long connectTimeoutMs = 2500;
    private long readTimeoutMs = 7000;

    /** Tentativas por envio em caso de erro de transporte. */
    private int sendAttempts = 3;
    private long sendRetryDelayMs = 400;
}
