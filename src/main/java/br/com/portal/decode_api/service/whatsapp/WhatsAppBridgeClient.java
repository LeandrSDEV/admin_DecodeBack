package br.com.portal.decode_api.service.whatsapp;

import br.com.portal.decode_api.config.WhatsAppBridgeProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP do whatsapp-bridge (Baileys) usado para enviar mensagens a partir
 * do painel Admin_Decode. Mesmo contrato do cliente usado no projeto restaurant
 * ({@code /api/bridge/send-text} com header {@code x-internal-token}).
 */
@Service
@RequiredArgsConstructor
public class WhatsAppBridgeClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBridgeClient.class);

    private final WhatsAppBridgeProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    private volatile RestTemplate restTemplate;

    public boolean isEnabled() {
        return properties.isEnabled()
                && properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()
                && properties.getToken() != null && !properties.getToken().isBlank();
    }

    /**
     * Envia texto simples. Lanca {@link IllegalStateException} em caso de falha.
     * Nao faz retry sobre erros 4xx/5xx reportados pelo bridge; apenas sobre erros
     * de transporte (connection refused, timeout, etc).
     */
    public void sendText(String phoneOrJid, String text) {
        if (!isEnabled()) {
            log.warn("whatsapp-bridge desabilitado ou nao configurado; mensagem descartada para={}", phoneOrJid);
            return;
        }
        String jid = WhatsAppJidUtils.ensureBrazilCountryCode(phoneOrJid);
        if (jid == null) {
            throw new IllegalArgumentException("Numero de WhatsApp invalido: " + phoneOrJid);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instance", properties.getInstance());
        body.put("remoteJid", jid);
        body.put("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-internal-token", properties.getToken());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = properties.getBaseUrl().replaceAll("/$", "") + "/api/bridge/send-text";
        int attempts = Math.max(1, properties.getSendAttempts());
        Exception lastError = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                getRestTemplate().exchange(url, HttpMethod.POST, entity, String.class);
                log.info("WhatsApp enviado com sucesso: para={} tentativa={}", jid, i);
                return;
            } catch (HttpStatusCodeException e) {
                String detail = e.getResponseBodyAsString();
                throw new IllegalStateException(
                        "Bridge respondeu erro " + e.getStatusCode() + ": " + detail, e);
            } catch (ResourceAccessException e) {
                lastError = e;
                log.warn("Falha de transporte no bridge (tentativa {}/{}): {}", i, attempts, e.getMessage());
                if (i < attempts) sleepQuietly(properties.getSendRetryDelayMs());
            } catch (Exception e) {
                lastError = e;
                break;
            }
        }
        throw new IllegalStateException(
                "Falha ao enviar mensagem via bridge apos " + attempts + " tentativas: "
                        + (lastError != null ? lastError.getMessage() : "desconhecido"),
                lastError);
    }

    private RestTemplate getRestTemplate() {
        RestTemplate current = restTemplate;
        if (current == null) {
            synchronized (this) {
                if (restTemplate == null) {
                    restTemplate = restTemplateBuilder
                            .setConnectTimeout(Duration.ofMillis(Math.max(1000, properties.getConnectTimeoutMs())))
                            .setReadTimeout(Duration.ofMillis(Math.max(1000, properties.getReadTimeoutMs())))
                            .build();
                }
                current = restTemplate;
            }
        }
        return current;
    }

    private void sleepQuietly(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
