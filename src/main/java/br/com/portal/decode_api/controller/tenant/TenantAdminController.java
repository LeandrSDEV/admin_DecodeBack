package br.com.portal.decode_api.controller.tenant;

import br.com.portal.decode_api.service.tenant.TenantProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints do painel admin do DECODE para gestão de tenants do backend
 * operacional (lanchonete). A autenticação aqui é o JWT admin normal —
 * o controller apenas encaminha para o endpoint interno do lanchonete com
 * um token S2S compartilhado.
 *
 * Role exigida: ADMIN.
 */
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TenantAdminController {

    private final TenantProxyService proxy;

    @GetMapping
    public ResponseEntity<String> list() {
        return proxy.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> get(@PathVariable Long id) {
        return proxy.get(id);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Map<String, Object> body) {
        return proxy.create(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return proxy.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> suspend(@PathVariable Long id) {
        return proxy.suspend(id);
    }

    @ExceptionHandler(TenantProxyService.TenancyProxyException.class)
    public ResponseEntity<Map<String, Object>> handleProxyError(TenantProxyService.TenancyProxyException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of(
                "error", "tenancy_proxy_error",
                "message", e.getMessage()
        ));
    }
}
