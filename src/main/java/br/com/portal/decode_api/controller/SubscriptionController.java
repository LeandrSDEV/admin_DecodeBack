package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.SubscriptionCancelRequest;
import br.com.portal.decode_api.dtos.SubscriptionRequest;
import br.com.portal.decode_api.dtos.SubscriptionResponse;
import br.com.portal.decode_api.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** Lista todas as assinaturas (com busca por nome do decode ou plano) */
    @GetMapping
    public Page<SubscriptionResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return subscriptionService.list(q, pageable);
    }

    /** Historico de assinaturas de um decode especifico */
    @GetMapping("/decode/{decodeId}")
    public Page<SubscriptionResponse> listByDecode(
            @PathVariable UUID decodeId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return subscriptionService.listByDecode(decodeId, pageable);
    }

    /** Assinatura ativa de um decode */
    @GetMapping("/decode/{decodeId}/active")
    public SubscriptionResponse getActive(@PathVariable UUID decodeId) {
        return subscriptionService.getActive(decodeId);
    }

    /** Criar nova assinatura */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.create(request);
    }

    /** Renovar assinatura (expira a anterior e cria nova) */
    @PostMapping("/{id}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse renew(@PathVariable UUID id, @Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.renew(id, request);
    }

    /** Cancelar assinatura */
    @PatchMapping("/{id}/cancel")
    public SubscriptionResponse cancel(@PathVariable UUID id, @RequestBody(required = false) SubscriptionCancelRequest request) {
        return subscriptionService.cancel(id, request);
    }

    /** Excluir assinatura (remove o registro definitivamente) */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        subscriptionService.delete(id);
    }
}
