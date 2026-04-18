package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.config.WhatsAppBridgeProperties;
import br.com.portal.decode_api.service.whatsapp.WhatsAppBridgeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controle da sessao Baileys dedicada do Admin_Decode (instance {@code decode-admin}).
 * Usada para parear o numero que envia notificacoes aos afiliados
 * (aprovacoes, recusas, etc).
 *
 * Role: ADMIN. Rotas sob {@code /api/admin/bridge}.
 */
@RestController
@RequestMapping("/api/admin/bridge")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBridgeController {

    private final WhatsAppBridgeClient bridgeClient;
    private final WhatsAppBridgeProperties properties;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> live = bridgeClient.safeFetchStatus();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", bridgeClient.isEnabled());
        response.put("instance", properties.getInstance());
        if (live == null) {
            response.put("reachable", false);
            response.put("message", "Bridge nao respondeu. Verifique se o servico esta rodando.");
        } else {
            response.put("reachable", true);
            response.putAll(live);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect() {
        return ResponseEntity.ok(bridgeClient.connect());
    }

    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restart() {
        return ResponseEntity.ok(bridgeClient.restart());
    }

    @PostMapping("/new-qr")
    public ResponseEntity<Map<String, Object>> newQr() {
        return ResponseEntity.ok(bridgeClient.newQr());
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        return ResponseEntity.ok(bridgeClient.disconnect());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(bridgeClient.logout());
    }
}
