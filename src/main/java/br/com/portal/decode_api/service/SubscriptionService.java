package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.SubscriptionCancelRequest;
import br.com.portal.decode_api.dtos.SubscriptionRequest;
import br.com.portal.decode_api.dtos.SubscriptionResponse;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.entity.SubscriptionEntity;
import br.com.portal.decode_api.enums.SubscriptionModule;
import br.com.portal.decode_api.enums.SubscriptionStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.DecodeRepository;
import br.com.portal.decode_api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DecodeRepository decodeRepository;
    private final br.com.portal.decode_api.service.tenant.TenantProxyService tenantProxyService;

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> list(String q, Pageable pageable) {
        return subscriptionRepository.search(q, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> listByDecode(UUID decodeId, Pageable pageable) {
        return subscriptionRepository.findByDecodeIdOrderByStartedAtDesc(decodeId, pageable).map(this::toResponse);
    }

    /**
     * Retorna a primeira assinatura ativa do decode (compatibilidade).
     * Para cenários com múltiplas assinaturas, use {@link #listActiveByDecode(UUID)}.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getActive(UUID decodeId) {
        SubscriptionEntity sub = subscriptionRepository
                .findFirstByDecodeIdAndStatusOrderByStartedAtDesc(decodeId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Assinatura ativa não encontrada para decode " + decodeId));
        return toResponse(sub);
    }

    /**
     * Retorna TODAS as assinaturas ativas de um decode.
     * Um decode pode ter uma assinatura MESA e outra DELIVERY simultaneamente.
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listActiveByDecode(UUID decodeId) {
        return subscriptionRepository.findAllByDecodeIdAndStatus(decodeId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retorna a assinatura ativa de um decode para um módulo específico.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveByModule(UUID decodeId, SubscriptionModule module) {
        SubscriptionEntity sub = subscriptionRepository
                .findFirstByDecodeIdAndModuleAndStatusOrderByStartedAtDesc(decodeId, module, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Assinatura ativa de " + module + " não encontrada para decode " + decodeId));
        return toResponse(sub);
    }

    /**
     * Computa o modo de operação efetivo com base nas assinaturas ativas
     * (e que não estejam com data expirada) de um decode.
     * Retorna "MESA", "DELIVERY", "BOTH" ou null se nenhuma assinatura ativa.
     */
    @Transactional(readOnly = true)
    public String computeEffectiveOperationMode(UUID decodeId) {
        List<SubscriptionEntity> activeSubs = subscriptionRepository
                .findAllByDecodeIdAndStatus(decodeId, SubscriptionStatus.ACTIVE);

        boolean hasMesa = false;
        boolean hasDelivery = false;

        for (SubscriptionEntity sub : activeSubs) {
            if (sub.isExpired()) continue; // status ACTIVE mas data já passou
            if (sub.getModule() == null) continue;
            if (sub.getModule().coversMesa()) hasMesa = true;
            if (sub.getModule().coversDelivery()) hasDelivery = true;
        }

        if (hasMesa && hasDelivery) return "BOTH";
        if (hasMesa) return "MESA";
        if (hasDelivery) return "DELIVERY";
        return null;
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest req) {
        DecodeEntity decode = null;
        if (req.decodeId() != null) {
            decode = decodeRepository.findById(req.decodeId())
                    .orElseThrow(() -> new EntityNotFoundException("Decode", req.decodeId()));

            // Valida conflito: não pode ter 2 assinaturas ativas do mesmo módulo
            validateNoConflict(req.decodeId(), req.module(), null);
        }

        LocalDateTime startedAt = req.startedAt() != null ? req.startedAt() : LocalDateTime.now();
        LocalDateTime expiresAt = req.expiresAt() != null
                ? req.expiresAt()
                : startedAt.plusDays(req.durationDays());

        SubscriptionEntity entity = SubscriptionEntity.builder()
                .decode(decode)
                .establishmentName(trimOrNull(req.establishmentName()))
                .clientName(trimOrNull(req.clientName()))
                .module(req.module())
                .planName(req.planName().trim())
                .price(req.price())
                .discountPct(req.discountPct() != null ? req.discountPct() : BigDecimal.ZERO)
                .durationDays(req.durationDays())
                .features(req.features())
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(startedAt)
                .expiresAt(expiresAt)
                .notes(req.notes())
                .build();

        SubscriptionEntity saved = subscriptionRepository.save(entity);
        syncSubscriptionToTenant(decode, saved.getExpiresAt());
        log.info("Plano criado: id={}, módulo={}, plano={}", saved.getId(), saved.getModule(), saved.getPlanName());
        return toResponse(saved);
    }

    /**
     * Valida que não há conflito de módulos ativos.
     * - Não pode ter MESA + COMPLETA ativas ao mesmo tempo
     * - Não pode ter DELIVERY + COMPLETA ativas ao mesmo tempo
     * - Não pode ter 2x o mesmo módulo ativo
     */
    private void validateNoConflict(UUID decodeId, SubscriptionModule newModule, UUID excludeId) {
        List<SubscriptionEntity> activeSubs = subscriptionRepository
                .findAllByDecodeIdAndStatus(decodeId, SubscriptionStatus.ACTIVE);

        for (SubscriptionEntity existing : activeSubs) {
            if (existing.isExpired()) continue;
            if (excludeId != null && existing.getId().equals(excludeId)) continue;

            SubscriptionModule existingModule = existing.getModule();

            // Mesmo módulo já ativo
            if (existingModule == newModule) {
                throw new IllegalStateException(
                        "Já existe uma assinatura ativa de " + existingModule + " para este decode. "
                                + "Renove ou cancele a existente antes de criar uma nova.");
            }

            // COMPLETA conflita com MESA ou DELIVERY individual
            if (newModule == SubscriptionModule.COMPLETA &&
                    (existingModule == SubscriptionModule.MESA || existingModule == SubscriptionModule.DELIVERY)) {
                throw new IllegalStateException(
                        "Não é possível criar assinatura Completa enquanto há assinatura de "
                                + existingModule + " ativa. Cancele ou aguarde o vencimento.");
            }
            if (existingModule == SubscriptionModule.COMPLETA &&
                    (newModule == SubscriptionModule.MESA || newModule == SubscriptionModule.DELIVERY)) {
                throw new IllegalStateException(
                        "Não é possível criar assinatura de " + newModule
                                + " enquanto há assinatura Completa ativa. Cancele ou aguarde o vencimento.");
            }
        }
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Renova apenas a assinatura do MESMO módulo.
     * A assinatura anterior é expirada e uma nova é criada.
     */
    @Transactional
    public SubscriptionResponse renew(UUID subscriptionId, SubscriptionRequest req) {
        SubscriptionEntity old = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", subscriptionId));

        // Marca a antiga como expirada (apenas se ativa)
        if (old.getStatus() == SubscriptionStatus.ACTIVE) {
            old.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(old);
        }

        // Cria nova assinatura mantendo o módulo
        SubscriptionModule module = req.module() != null ? req.module() : old.getModule();

        LocalDateTime startedAt = req.startedAt() != null ? req.startedAt() : LocalDateTime.now();
        LocalDateTime expiresAt = req.expiresAt() != null
                ? req.expiresAt()
                : startedAt.plusDays(req.durationDays());

        SubscriptionEntity renewed = SubscriptionEntity.builder()
                .decode(old.getDecode())
                .establishmentName(trimOrNull(req.establishmentName()) != null
                        ? trimOrNull(req.establishmentName()) : old.getEstablishmentName())
                .clientName(trimOrNull(req.clientName()) != null
                        ? trimOrNull(req.clientName()) : old.getClientName())
                .module(module)
                .planName(req.planName().trim())
                .price(req.price())
                .discountPct(req.discountPct() != null ? req.discountPct() : BigDecimal.ZERO)
                .durationDays(req.durationDays())
                .features(req.features())
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(startedAt)
                .expiresAt(expiresAt)
                .notes(req.notes())
                .build();

        SubscriptionEntity saved = subscriptionRepository.save(renewed);
        syncSubscriptionToTenant(old.getDecode(), saved.getExpiresAt());
        log.info("Assinatura renovada: id={}, módulo={}, anterior={}", saved.getId(), saved.getModule(), subscriptionId);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id, SubscriptionCancelRequest req) {
        SubscriptionEntity entity = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));

        entity.setStatus(SubscriptionStatus.CANCELLED);
        entity.setCancelledAt(LocalDateTime.now());
        entity.setCancelReason(req != null ? req.reason() : null);

        log.info("Assinatura cancelada: id={}, módulo={}", id, entity.getModule());
        return toResponse(subscriptionRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        SubscriptionEntity entity = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));
        subscriptionRepository.delete(entity);
        log.info("Assinatura deletada: id={}", id);
    }

    private SubscriptionResponse toResponse(SubscriptionEntity s) {
        DecodeEntity d = s.getDecode();
        return new SubscriptionResponse(
                s.getId(),
                d != null ? d.getId() : null,
                d != null ? d.getName() : null,
                d != null ? d.getCode() : null,
                s.getEstablishmentName(),
                s.getClientName(),
                s.getModule(),
                s.getPlanName(),
                s.getPrice(),
                s.getDiscountPct(),
                s.getDurationDays(),
                s.getFeatures(),
                s.getStatus(),
                s.isActive(),
                s.getStartedAt(),
                s.getExpiresAt(),
                s.getCancelledAt(),
                s.getCancelReason(),
                s.getNotes(),
                s.getCreatedAt()
        );
    }

    /**
     * Sincroniza a data de expiração da assinatura com o tenant no backend operacional.
     * Usa o tenant_id armazenado no decode para identificar qual tenant atualizar.
     */
    private void syncSubscriptionToTenant(DecodeEntity decode, LocalDateTime expiresAt) {
        if (decode == null || decode.getTenantId() == null) {
            log.debug("Sync de assinatura ignorado: decode ou tenantId nulo");
            return;
        }
        try {
            String isoDate = expiresAt.atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toString();
            tenantProxyService.syncSubscriptionExpiration(decode.getTenantId(), isoDate);
        } catch (Exception e) {
            log.warn("Falha ao sincronizar assinatura para tenant {}: {}",
                    decode.getTenantId(), e.getMessage());
        }
    }
}
