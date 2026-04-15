package br.com.portal.decode_api.service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente S2S para o endpoint `/api/internal/metrics` do lanchonete.
 * Retorna o JSON desserializado em Map (estrutura tipada flexível, evita
 * duplicar DTOs entre os 2 backends — o painel admin não precisa de tipos
 * estritos no contrato).
 *
 * Reusa as mesmas envs `app.tenancy.baseUrl` e `app.tenancy.serviceToken`
 * configuradas no TenantProxyService.
 */
@Service
public class MetricsProxyService {

    private static final Logger log = LoggerFactory.getLogger(MetricsProxyService.class);
    private static final String PATH = "/api/internal/metrics";

    private final RestClient client;
    private final ObjectMapper mapper;
    private final boolean configured;

    public MetricsProxyService(
            @Value("${app.tenancy.baseUrl:}") String baseUrl,
            @Value("${app.tenancy.serviceToken:}") String serviceToken,
            ObjectMapper mapper
    ) {
        String trimmedBase = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        String trimmedToken = serviceToken == null ? "" : serviceToken.trim();
        this.configured = !trimmedBase.isBlank() && !trimmedToken.isBlank();
        this.mapper = mapper;

        if (!configured) {
            log.warn("[metrics-proxy] APP_TENANCY_BASE_URL ou APP_TENANCY_SERVICE_TOKEN não configurados");
            this.client = RestClient.builder().build();
        } else {
            this.client = RestClient.builder()
                    .baseUrl(trimmedBase + PATH)
                    .defaultHeader("X-Service-Token", trimmedToken)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> aggregate() {
        if (!configured) {
            log.warn("[metrics-proxy] retornando vazio porque proxy não está configurado");
            return Map.of("generatedAt", null, "tenantCount", 0, "tenants", java.util.List.of());
        }
        try {
            String body = client.get().retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return Map.of("tenantCount", 0, "tenants", java.util.List.of());
            }
            return mapper.readValue(body, Map.class);
        } catch (HttpStatusCodeException e) {
            log.warn("[metrics-proxy] upstream retornou {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Map.of("tenantCount", 0, "tenants", java.util.List.of(),
                    "error", "upstream " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[metrics-proxy] falha ao chamar upstream", e);
            return Map.of("tenantCount", 0, "tenants", java.util.List.of(),
                    "error", e.getMessage());
        }
    }
}
