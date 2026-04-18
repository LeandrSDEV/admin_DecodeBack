package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.dtos.affiliate.AffiliateDecodeSubmissionRequest;
import br.com.portal.decode_api.dtos.affiliate.AffiliateDecodeSubmissionResponse;
import br.com.portal.decode_api.entity.AffiliateDecodeSubmissionEntity;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.entity.SubscriptionEntity;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.AffiliateDecodeSubmissionStatus;
import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.enums.DecodeStatus;
import br.com.portal.decode_api.enums.SubscriptionStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.AffiliateDecodeSubmissionRepository;
import br.com.portal.decode_api.repository.AffiliateRepository;
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

/**
 * Gerencia propostas de estabelecimento submetidas pelo afiliado no portal.
 *
 * Fluxo:
 * - Afiliado (ACTIVE) submete via portal   -> status = PENDING
 * - Admin revisa e aprova                  -> cria Decode + Subscription + Referral CONVERTED
 * - Admin revisa e rejeita                 -> status = REJECTED, registra motivo
 */
@Service
@RequiredArgsConstructor
public class AffiliateDecodeSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateDecodeSubmissionService.class);

    private final AffiliateDecodeSubmissionRepository submissionRepository;
    private final AffiliateRepository affiliateRepository;
    private final DecodeRepository decodeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AffiliateReferralService referralService;

    // ==============================================================
    // Submissao pelo afiliado
    // ==============================================================
    @Transactional
    public AffiliateDecodeSubmissionResponse submit(UUID affiliateId, AffiliateDecodeSubmissionRequest req) {
        AffiliateEntity affiliate = affiliateRepository.findById(affiliateId)
                .orElseThrow(() -> new EntityNotFoundException("Affiliate", affiliateId));

        if (affiliate.getStatus() != AffiliateStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Somente afiliados ativos podem submeter novos estabelecimentos. Status atual: "
                            + affiliate.getStatus());
        }

        AffiliateDecodeSubmissionEntity entity = AffiliateDecodeSubmissionEntity.builder()
                .affiliate(affiliate)
                .establishmentName(req.establishmentName().trim())
                .city(req.city().trim())
                .state(trimOrNull(req.state()))
                .cnpj(trimOrNull(req.cnpj()))
                .contactName(req.contactName().trim())
                .contactEmail(trimOrNull(req.contactEmail()))
                .contactPhone(normalizePhone(req.contactPhone()))
                .estimatedUsersCount(req.estimatedUsersCount())
                .estimatedMonthlyRevenue(req.estimatedMonthlyRevenue())
                .planModule(req.planModule())
                .planName(req.planName().trim())
                .planPrice(req.planPrice())
                .planDiscountPct(req.planDiscountPct() != null ? req.planDiscountPct() : BigDecimal.ZERO)
                .planDurationDays(req.planDurationDays())
                .planFeatures(trimOrNull(req.planFeatures()))
                .notes(trimOrNull(req.notes()))
                .status(AffiliateDecodeSubmissionStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        AffiliateDecodeSubmissionEntity saved = submissionRepository.save(entity);
        log.info("Afiliado {} submeteu proposta de estabelecimento '{}' (id={})",
                affiliateId, saved.getEstablishmentName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AffiliateDecodeSubmissionResponse> listMine(UUID affiliateId, Pageable pageable) {
        return submissionRepository
                .findByAffiliateIdOrderBySubmittedAtDesc(affiliateId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AffiliateDecodeSubmissionResponse> listAll(String q,
                                                            AffiliateDecodeSubmissionStatus status,
                                                            Pageable pageable) {
        return submissionRepository.search(q, status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AffiliateDecodeSubmissionResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return submissionRepository.countByStatus(AffiliateDecodeSubmissionStatus.PENDING);
    }

    // ==============================================================
    // Aprovacao (admin) -> cria Decode + Subscription + Referral
    // ==============================================================
    @Transactional
    public AffiliateDecodeSubmissionResponse approve(UUID id, UserEntity reviewer) {
        AffiliateDecodeSubmissionEntity sub = require(id);
        if (sub.getStatus() != AffiliateDecodeSubmissionStatus.PENDING) {
            throw new IllegalStateException(
                    "Solicitacao nao esta pendente (status atual: " + sub.getStatus() + ")");
        }

        AffiliateEntity affiliate = sub.getAffiliate();
        if (affiliate.getStatus() != AffiliateStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Afiliado responsavel nao esta ATIVO (" + affiliate.getStatus()
                            + "); nao e possivel aprovar esta solicitacao.");
        }

        LocalDateTime now = LocalDateTime.now();

        DecodeEntity decode = DecodeEntity.builder()
                .name(sub.getEstablishmentName())
                .city(sub.getCity())
                .status(DecodeStatus.ACTIVE)
                .usersCount(sub.getEstimatedUsersCount() != null ? sub.getEstimatedUsersCount() : 0)
                .monthlyRevenue(sub.getEstimatedMonthlyRevenue() != null
                        ? sub.getEstimatedMonthlyRevenue() : BigDecimal.ZERO)
                .affiliate(affiliate)
                .affiliateAttachedAt(now)
                .build();
        DecodeEntity savedDecode = decodeRepository.save(decode);
        log.info("Decode criado via aprovacao de submission {}: decode={}, afiliado={}",
                id, savedDecode.getId(), affiliate.getId());

        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .decode(savedDecode)
                .establishmentName(sub.getEstablishmentName())
                .clientName(sub.getContactName())
                .module(sub.getPlanModule())
                .planName(sub.getPlanName())
                .price(sub.getPlanPrice())
                .discountPct(sub.getPlanDiscountPct())
                .durationDays(sub.getPlanDurationDays())
                .features(sub.getPlanFeatures())
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .expiresAt(now.plusDays(sub.getPlanDurationDays()))
                .notes(sub.getNotes())
                .build();
        SubscriptionEntity savedSub = subscriptionRepository.save(subscription);
        log.info("Subscription criada para decode {} via submission {}: sub={}, modulo={}, plano={}",
                savedDecode.getId(), id, savedSub.getId(), savedSub.getModule(), savedSub.getPlanName());

        try {
            referralService.markConverted(affiliate.getId(), null, savedDecode);
        } catch (Exception e) {
            log.warn("Falha ao registrar referral CONVERTED para submission {}: {}", id, e.getMessage());
        }

        sub.setStatus(AffiliateDecodeSubmissionStatus.APPROVED);
        sub.setReviewedAt(now);
        sub.setReviewedBy(reviewer);
        sub.setDecode(savedDecode);
        sub.setSubscription(savedSub);

        return toResponse(submissionRepository.save(sub));
    }

    // ==============================================================
    // Rejeicao (admin)
    // ==============================================================
    @Transactional
    public AffiliateDecodeSubmissionResponse reject(UUID id, String reason, UserEntity reviewer) {
        AffiliateDecodeSubmissionEntity sub = require(id);
        if (sub.getStatus() != AffiliateDecodeSubmissionStatus.PENDING) {
            throw new IllegalStateException(
                    "Solicitacao nao esta pendente (status atual: " + sub.getStatus() + ")");
        }
        sub.setStatus(AffiliateDecodeSubmissionStatus.REJECTED);
        sub.setReviewedAt(LocalDateTime.now());
        sub.setReviewedBy(reviewer);
        sub.setRejectionReason(reason);
        log.info("Submission {} rejeitada por {}: {}", id, reviewer.getEmail(), reason);
        return toResponse(submissionRepository.save(sub));
    }

    // ==============================================================
    // Helpers
    // ==============================================================
    private AffiliateDecodeSubmissionEntity require(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AffiliateDecodeSubmission", id));
    }

    public AffiliateDecodeSubmissionResponse toResponse(AffiliateDecodeSubmissionEntity s) {
        AffiliateEntity aff = s.getAffiliate();
        UserEntity reviewer = s.getReviewedBy();
        DecodeEntity d = s.getDecode();
        SubscriptionEntity sub = s.getSubscription();

        return new AffiliateDecodeSubmissionResponse(
                s.getId(),
                aff != null ? aff.getId() : null,
                aff != null ? aff.getName() : null,
                aff != null ? aff.getRefCode() : null,
                s.getEstablishmentName(),
                s.getCity(),
                s.getState(),
                s.getCnpj(),
                s.getContactName(),
                s.getContactEmail(),
                s.getContactPhone(),
                s.getEstimatedUsersCount(),
                s.getEstimatedMonthlyRevenue(),
                s.getPlanModule(),
                s.getPlanName(),
                s.getPlanPrice(),
                s.getPlanDiscountPct(),
                s.getPlanDurationDays(),
                s.getPlanFeatures(),
                s.getNotes(),
                s.getStatus(),
                s.getSubmittedAt(),
                s.getReviewedAt(),
                reviewer != null ? reviewer.getName() : null,
                s.getRejectionReason(),
                d != null ? d.getId() : null,
                d != null ? d.getCode() : null,
                sub != null ? sub.getId() : null,
                s.getCreatedAt()
        );
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9+]", "");
    }
}
