package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.SubscriptionCancelRequest;
import br.com.portal.decode_api.dtos.SubscriptionRequest;
import br.com.portal.decode_api.dtos.SubscriptionResponse;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.entity.SubscriptionEntity;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DecodeRepository decodeRepository;

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> list(String q, Pageable pageable) {
        return subscriptionRepository.search(q, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> listByDecode(UUID decodeId, Pageable pageable) {
        return subscriptionRepository.findByDecodeIdOrderByStartedAtDesc(decodeId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getActive(UUID decodeId) {
        SubscriptionEntity sub = subscriptionRepository
                .findFirstByDecodeIdAndStatusOrderByStartedAtDesc(decodeId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Assinatura ativa não encontrada para decode " + decodeId));
        return toResponse(sub);
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest req) {
        DecodeEntity decode = null;
        if (req.decodeId() != null) {
            decode = decodeRepository.findById(req.decodeId())
                    .orElseThrow(() -> new EntityNotFoundException("Decode", req.decodeId()));
        }

        LocalDateTime startedAt = req.startedAt() != null ? req.startedAt() : LocalDateTime.now();
        LocalDateTime expiresAt = req.expiresAt() != null
                ? req.expiresAt()
                : startedAt.plusDays(req.durationDays());

        SubscriptionEntity entity = SubscriptionEntity.builder()
                .decode(decode)
                .establishmentName(trimOrNull(req.establishmentName()))
                .clientName(trimOrNull(req.clientName()))
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
        log.info("Plano criado: id={}, plano={}", saved.getId(), saved.getPlanName());
        return toResponse(saved);
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional
    public SubscriptionResponse renew(UUID subscriptionId, SubscriptionRequest req) {
        SubscriptionEntity old = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", subscriptionId));

        // marca a antiga como expirada
        if (old.getStatus() == SubscriptionStatus.ACTIVE) {
            old.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(old);
        }

        // cria nova assinatura
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
        log.info("Assinatura renovada: id={}, anterior={}", saved.getId(), subscriptionId);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id, SubscriptionCancelRequest req) {
        SubscriptionEntity entity = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));

        entity.setStatus(SubscriptionStatus.CANCELLED);
        entity.setCancelledAt(LocalDateTime.now());
        entity.setCancelReason(req != null ? req.reason() : null);

        log.info("Assinatura cancelada: id={}", id);
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
}
