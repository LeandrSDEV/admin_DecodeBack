package br.com.portal.decode_api.service.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Encaminha requisições de gestão de tenants para o backend operacional
 * (lanchonetev14.0) usando o endpoint interno `/api/internal/tenants/**`
 * autenticado por token pré-compartilhado (X-Service-Token).
 *
 * Env vars exigidas:
 *   - APP_TENANCY_BASE_URL        (ex: https://decode.portaledtech.com)
 *   - APP_TENANCY_SERVICE_TOKEN   (mesmo valor configurado em lanchonetev14.0)
 */
@Service
public class TenantProxyService {

    private static final Logger log = LoggerFactory.getLogger(TenantProxyService.class);
    private static final String BASE_PATH = "/api/internal/tenants";

    private final RestClient client;
    private final boolean configured;

    public TenantProxyService(
            @Value("${app.tenancy.baseUrl:}") String baseUrl,
            @Value("${app.tenancy.serviceToken:}") String serviceToken
    ) {
        String trimmedBase = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        String trimmedToken = serviceToken == null ? "" : serviceToken.trim();
        this.configured = !trimmedBase.isBlank() && !trimmedToken.isBlank();

        if (!configured) {
            log.warn("[tenancy-proxy] APP_TENANCY_BASE_URL ou APP_TENANCY_SERVICE_TOKEN não configurados — "
                    + "o painel de tenants vai falhar até isso ser definido");
            this.client = RestClient.builder().build();
        } else {
            this.client = RestClient.builder()
                    .baseUrl(trimmedBase + BASE_PATH)
                    .defaultHeader("X-Service-Token", trimmedToken)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new TenancyProxyException(500,
                    "O proxy de tenants não está configurado. Defina APP_TENANCY_BASE_URL e APP_TENANCY_SERVICE_TOKEN.");
        }
    }

    public ResponseEntity<String> list() {
        ensureConfigured();
        return exchange(() -> client.get().retrieve().toEntity(String.class));
    }

    public ResponseEntity<String> get(Long id) {
        ensureConfigured();
        return exchange(() -> client.get().uri("/{id}", id).retrieve().toEntity(String.class));
    }

    public ResponseEntity<String> create(Map<String, Object> body) {
        ensureConfigured();
        return exchange(() -> client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    public ResponseEntity<String> update(Long id, Map<String, Object> body) {
        ensureConfigured();
        return exchange(() -> client.put()
                .uri("/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class));
    }

    public ResponseEntity<String> suspend(Long id) {
        ensureConfigured();
        return exchange(() -> client.delete()
                .uri("/{id}", id)
                .retrieve()
                .toEntity(String.class));
    }

    private ResponseEntity<String> exchange(java.util.function.Supplier<ResponseEntity<String>> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            String body = e.getResponseBodyAsString();
            log.warn("[tenancy-proxy] upstream retornou {}: {}", status, body);
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null || body.isBlank() ? "{\"error\":\"upstream error\"}" : body);
        } catch (Exception e) {
            log.error("[tenancy-proxy] falha ao chamar upstream", e);
            throw new TenancyProxyException(502, "Falha ao comunicar com o backend operacional: " + e.getMessage());
        }
    }

    public static class TenancyProxyException extends RuntimeException {
        private final int status;

        public TenancyProxyException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
