package br.com.portal.decode_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envia notificações push via Pushover (https://pushover.net/api).
 * <p>
 * Configuração em application.yaml:
 * <pre>
 * app.pushover.enabled: true
 * app.pushover.token: YOUR_APP_TOKEN
 * app.pushover.user: YOUR_USER_KEY
 * </pre>
 */
@Service
public class PushoverService {

    private static final Logger log = LoggerFactory.getLogger(PushoverService.class);
    private static final String API_URL = "https://api.pushover.net/1/messages.json";

    @Value("${app.pushover.enabled:false}")
    private boolean enabled;

    @Value("${app.pushover.token:}")
    private String token;

    @Value("${app.pushover.user:}")
    private String userKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Envia notificação ao Pushover. Se desabilitado ou sem credenciais, faz log e retorna.
     *
     * @param title   título da notificação
     * @param message corpo da mensagem
     * @param priority -2 (silencioso) a 2 (emergência). 0 = normal, 1 = alta
     */
    public void send(String title, String message, int priority) {
        if (!enabled || token.isBlank() || userKey.isBlank()) {
            log.debug("Pushover desabilitado ou sem credenciais. title={}", title);
            return;
        }

        try {
            String body = "token=" + enc(token)
                    + "&user=" + enc(userKey)
                    + "&title=" + enc(title)
                    + "&message=" + enc(message)
                    + "&priority=" + priority
                    + "&sound=siren";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("Pushover enviado: title={}, status={}", title, resp.statusCode());
            } else {
                log.warn("Pushover falhou: status={}, body={}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.error("Erro ao enviar Pushover: {}", e.getMessage());
        }
    }

    /** Atalho para notificação de incidente (prioridade alta) */
    public void notifyIncidentOpened(String siteName, String siteUrl, String errorMessage) {
        String title = "SISTEMA FORA DO AR: " + siteName;
        String msg = "URL: " + siteUrl
                + "\nErro: " + (errorMessage != null ? errorMessage : "sem resposta")
                + "\nHorário: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        send(title, msg, 1);
    }

    /** Atalho para notificação de recuperação */
    public void notifyIncidentClosed(String siteName, String siteUrl) {
        String title = "RECUPERADO: " + siteName;
        String msg = "URL: " + siteUrl
                + "\nStatus: Sistema voltou ao normal"
                + "\nHorário: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        send(title, msg, 0);
    }

    /** Atalho para alerta de SSL prestes a expirar */
    public void notifySslExpiring(String siteName, String siteUrl, int daysLeft) {
        String title = "SSL EXPIRANDO: " + siteName;
        String msg = "URL: " + siteUrl
                + "\nDias restantes: " + daysLeft
                + "\nRenove o certificado o mais rápido possível.";
        send(title, msg, daysLeft <= 3 ? 1 : 0);
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
