package br.com.portal.decode_api.service.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Encaminha requisições de gestão de tenants para o backend operacional
 * (lanchonetev14.0) usando o endpoint interno `/api/internal/tenants/**`
 * autenticado por token pré-compartilhado (X-Service-Token).
 *
 * Env vars exigidas:
 *   - APP_TENANCY_BASE_URL        (ex: https://decode.portaledtech.com)
 *   - APP_TENANCY_SERVICE_TOKEN   (mesmo valor configurado em lanchonetev14.0)
 *
 * IMPORTANTE: a resposta devolvida para o cliente é uma ResponseEntity
 * construída do zero — só o body é propagado do upstream. NÃO repassamos
 * os headers do upstream porque isso vaza detalhes internos (tipo
 * X-Tenant-Resolved do filtro multi-tenant) e pode incluir headers
 * HTTP/2 inválidos em HTTP/1.1 (ex: ":status"), que fazem o Traefik
 * rejeitar a resposta com "Internal Server Error".
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
        return exchange(() -> client.get().retrieve().body(String.class));
    }

    public ResponseEntity<String> get(Long id) {
        ensureConfigured();
        return exchange(() -> client.get().uri("/{id}", id).retrieve().body(String.class));
    }

    public ResponseEntity<String> create(Map<String, Object> body) {
        ensureConfigured();
        return exchangeCreated(() -> client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
    }

    public ResponseEntity<String> update(Long id, Map<String, Object> body) {
        ensureConfigured();
        return exchange(() -> client.put()
                .uri("/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
    }

    public ResponseEntity<String> suspend(Long id) {
        ensureConfigured();
        try {
            client.delete().uri("/{id}", id).retrieve().toBodilessEntity();
            return ResponseEntity.noContent().build();
        } catch (HttpStatusCodeException e) {
            return upstreamError(e);
        } catch (Exception e) {
            log.error("[tenancy-proxy] falha ao chamar upstream", e);
            throw new TenancyProxyException(502, "Falha ao comunicar com o backend operacional: " + e.getMessage());
        }
    }

    public ResponseEntity<String> getConfig(Long id) {
        ensureConfigured();
        return exchange(() -> client.get().uri("/{id}/config", id).retrieve().body(String.class));
    }

    public ResponseEntity<String> updateConfig(Long id, Map<String, Object> body) {
        ensureConfigured();
        return exchange(() -> client.put()
                .uri("/{id}/config", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
    }

    /**
     * Executa a chamada upstream e devolve uma ResponseEntity NOVA com
     * Content-Type application/json — sem propagar headers do upstream.
     */
    private ResponseEntity<String> exchange(Supplier<String> call) {
        try {
            String body = call.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "null" : body);
        } catch (HttpStatusCodeException e) {
            return upstreamError(e);
        } catch (Exception e) {
            log.error("[tenancy-proxy] falha ao chamar upstream", e);
            throw new TenancyProxyException(502, "Falha ao comunicar com o backend operacional: " + e.getMessage());
        }
    }

    /**
     * Variante para POST create que devolve 201 Created.
     */
    private ResponseEntity<String> exchangeCreated(Supplier<String> call) {
        try {
            String body = call.get();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "null" : body);
        } catch (HttpStatusCodeException e) {
            return upstreamError(e);
        } catch (Exception e) {
            log.error("[tenancy-proxy] falha ao chamar upstream", e);
            throw new TenancyProxyException(502, "Falha ao comunicar com o backend operacional: " + e.getMessage());
        }
    }

    private ResponseEntity<String> upstreamError(HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        log.warn("[tenancy-proxy] upstream retornou {}: {}", e.getStatusCode(), body);
        return ResponseEntity.status(e.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body == null || body.isBlank() ? "{\"error\":\"upstream error\"}" : body);
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
