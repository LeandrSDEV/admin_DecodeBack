package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.dtos.affiliate.*;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
import br.com.portal.decode_api.enums.AffiliateReferralStatus;
import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.enums.PixKeyType;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.AffiliateReferralRepository;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.repository.DecodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AffiliateService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateService.class);
    private static final SecureRandom RNG = new SecureRandom();
    private static final String REF_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem I,O,0,1
    private static final int REF_CODE_LENGTH = 7;

    private final AffiliateRepository affiliateRepository;
    private final AffiliateReferralRepository referralRepository;
    private final AffiliateCommissionRepository commissionRepository;
    private final DecodeRepository decodeRepository;
    private final br.com.portal.decode_api.repository.SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // -----------------------------------------------------------------
    // Cadastro publico (landing -> POST /api/public/affiliates/apply)
    // -----------------------------------------------------------------
    @Transactional
    public AffiliateApplyResponse apply(AffiliateApplyRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (affiliateRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Este email ja esta cadastrado no programa de afiliados.");
        }

        AffiliateEntity a = AffiliateEntity.builder()
                .refCode(generateUniqueRefCode(req.name()))
                .name(req.name().trim())
                .email(email)
                .whatsapp(normalizePhone(req.whatsapp()))
                .cpf(trimOrNull(req.cpf()))
                .city(trimOrNull(req.city()))
                .state(trimOrNull(req.state()))
                .pixKeyType(parsePixKeyType(req.pixKeyType()))
                .pixKey(trimOrNull(req.pixKey()))
                .status(AffiliateStatus.PENDING)
                .mustChangePw(true)
                .termsAcceptedAt(LocalDateTime.now())
                .termsAcceptedVersion(req.termsVersion())
                .build();

        AffiliateEntity saved = affiliateRepository.save(a);
        log.info("Novo cadastro de afiliado (PENDING): id={}, refCode={}, email={}", saved.getId(), saved.getRefCode(), saved.getEmail());

        return new AffiliateApplyResponse(
                saved.getStatus().name(),
                "Cadastro recebido! Vamos revisar e entrar em contato em ate 24h.",
                saved.getRefCode()
        );
    }

    // -----------------------------------------------------------------
    // Criacao manual (admin)
    // -----------------------------------------------------------------
    @Transactional
    public AffiliateResponse createByAdmin(AffiliateCreateRequest req, UserEntity creator) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (affiliateRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email ja cadastrado: " + email);
        }

        String refCode = (req.refCode() == null || req.refCode().isBlank())
                ? generateUniqueRefCode(req.name())
                : req.refCode().trim().toUpperCase(Locale.ROOT);
        if (affiliateRepository.existsByRefCode(refCode)) {
            throw new IllegalArgumentException("refCode ja em uso: " + refCode);
        }

        AffiliateEntity a = AffiliateEntity.builder()
                .refCode(refCode)
                .name(req.name().trim())
                .email(email)
                .whatsapp(normalizePhone(req.whatsapp()))
                .cpf(trimOrNull(req.cpf()))
                .city(trimOrNull(req.city()))
                .state(trimOrNull(req.state()))
                .pixKeyType(parsePixKeyType(req.pixKeyType()))
                .pixKey(trimOrNull(req.pixKey()))
                .customCommissionRate(req.customCommissionRate())
                .status(AffiliateStatus.ACTIVE)
                .approvedAt(LocalDateTime.now())
                .approvedBy(creator)
                .mustChangePw(req.initialPassword() == null || req.initialPassword().isBlank())
                .passwordHash(req.initialPassword() != null && !req.initialPassword().isBlank()
                        ? passwordEncoder.encode(req.initialPassword())
                        : null)
                .build();

        AffiliateEntity saved = affiliateRepository.save(a);
        log.info("Afiliado criado pelo admin: id={}, refCode={}", saved.getId(), saved.getRefCode());

        if (req.decodeId() != null) {
            DecodeEntity decode = decodeRepository.findById(req.decodeId())
                    .orElseThrow(() -> new EntityNotFoundException("Decode", req.decodeId()));
            decode.setAffiliate(saved);
            decode.setAffiliateAttachedAt(LocalDateTime.now());
            decodeRepository.save(decode);
            log.info("Decode {} vinculado ao afiliado {}", decode.getId(), saved.getId());
        }

        if (req.initialPassword() != null && !req.initialPassword().isBlank()) {
            eventPublisher.publishEvent(new AffiliateApprovedEvent(
                    saved.getId(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getWhatsapp(),
                    saved.getRefCode(),
                    req.initialPassword()
            ));
        }

        return toResponse(saved);
    }

    // -----------------------------------------------------------------
    // Aprovacao de cadastro pendente
    // -----------------------------------------------------------------
    @Transactional
    public AffiliateResponse approve(UUID id, AffiliateApproveRequest req, UserEntity approver) {
        AffiliateEntity a = requireAffiliate(id);
        if (a.getStatus() != AffiliateStatus.PENDING) {
            throw new IllegalStateException("Afiliado nao esta em status PENDING: " + a.getStatus());
        }

        a.setStatus(AffiliateStatus.ACTIVE);
        a.setApprovedAt(LocalDateTime.now());
        a.setApprovedBy(approver);
        a.setPasswordHash(passwordEncoder.encode(req.initialPassword()));
        a.setMustChangePw(true);
        if (req.notes() != null && !req.notes().isBlank()) {
            a.setNotes(req.notes());
        }

        AffiliateEntity saved = affiliateRepository.save(a);
        log.info("Afiliado aprovado: id={}, por={}", saved.getId(), approver.getEmail());

        eventPublisher.publishEvent(new AffiliateApprovedEvent(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getWhatsapp(),
                saved.getRefCode(),
                req.initialPassword()
        ));

        return toResponse(saved);
    }

    @Transactional
    public AffiliateResponse update(UUID id, AffiliateUpdateRequest req) {
        AffiliateEntity a = requireAffiliate(id);
        if (req.name() != null) a.setName(req.name().trim());
        if (req.whatsapp() != null) a.setWhatsapp(normalizePhone(req.whatsapp()));
        if (req.cpf() != null) a.setCpf(trimOrNull(req.cpf()));
        if (req.city() != null) a.setCity(trimOrNull(req.city()));
        if (req.state() != null) a.setState(trimOrNull(req.state()));
        if (req.pixKeyType() != null) a.setPixKeyType(parsePixKeyType(req.pixKeyType()));
        if (req.pixKey() != null) a.setPixKey(trimOrNull(req.pixKey()));
        if (req.status() != null) a.setStatus(req.status());
        if (req.customCommissionRate() != null) a.setCustomCommissionRate(req.customCommissionRate());
        if (req.notes() != null) a.setNotes(req.notes());

        return toResponse(affiliateRepository.save(a));
    }

    @Transactional
    public void delete(UUID id) {
        AffiliateEntity a = requireAffiliate(id);

        boolean hasPaidCommission = commissionRepository
                .sumAmountByAffiliateAndStatus(id, AffiliateCommissionStatus.PAID)
                .compareTo(BigDecimal.ZERO) > 0;
        if (hasPaidCommission) {
            throw new IllegalStateException(
                    "Afiliado possui comissoes pagas e nao pode ser excluido. Suspenda ou banha o cadastro.");
        }

        List<DecodeEntity> linkedDecodes = decodeRepository.findByAffiliateId(id);
        for (DecodeEntity d : linkedDecodes) {
            d.setAffiliate(null);
            d.setAffiliateAttachedAt(null);
        }
        if (!linkedDecodes.isEmpty()) {
            decodeRepository.saveAll(linkedDecodes);
        }

        commissionRepository.deleteAll(commissionRepository.findByAffiliateId(id));
        referralRepository.deleteAll(referralRepository.findByAffiliateId(id));

        affiliateRepository.delete(a);
        log.info("Afiliado deletado: id={}, decodes desvinculados={}", id, linkedDecodes.size());
    }

    @Transactional
    public void suspend(UUID id, String reason) {
        AffiliateEntity a = requireAffiliate(id);
        a.setStatus(AffiliateStatus.SUSPENDED);
        a.setNotes((a.getNotes() != null ? a.getNotes() + "\n" : "") + "[SUSPENSO] " + reason);
        affiliateRepository.save(a);
    }

    @Transactional
    public void reactivate(UUID id) {
        AffiliateEntity a = requireAffiliate(id);
        if (a.getStatus() == AffiliateStatus.BANNED) {
            throw new IllegalStateException("Afiliado banido nao pode ser reativado.");
        }
        a.setStatus(AffiliateStatus.ACTIVE);
        affiliateRepository.save(a);
    }

    // -----------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------
    @Transactional(readOnly = true)
    public AffiliateResponse get(UUID id) {
        return toResponse(requireAffiliate(id));
    }

    @Transactional(readOnly = true)
    public Page<AffiliateResponse> list(String q, AffiliateStatus status, Pageable pageable) {
        return affiliateRepository.search(q, status, pageable).map(this::toResponse);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------
    public AffiliateEntity requireAffiliate(UUID id) {
        return affiliateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Affiliate", id));
    }

    public AffiliateResponse toResponse(AffiliateEntity a) {
        long totalRefs = referralRepository.countByAffiliateIdAndStatus(a.getId(), AffiliateReferralStatus.CONVERTED)
                + referralRepository.countByAffiliateIdAndStatus(a.getId(), AffiliateReferralStatus.CHURNED)
                + referralRepository.countByAffiliateIdAndStatus(a.getId(), AffiliateReferralStatus.LEAD);
        long active = decodeRepository.countActiveByAffiliateId(a.getId());
        BigDecimal montante = subscriptionRepository.sumActivePlanAmountByAffiliateId(a.getId());
        BigDecimal pending = commissionRepository.sumAmountByAffiliateAndStatus(a.getId(), AffiliateCommissionStatus.PENDING)
                .add(commissionRepository.sumAmountByAffiliateAndStatus(a.getId(), AffiliateCommissionStatus.APPROVED));

        return new AffiliateResponse(
                a.getId(),
                a.getRefCode(),
                a.getName(),
                a.getEmail(),
                a.getWhatsapp(),
                a.getCpf(),
                a.getCity(),
                a.getState(),
                a.getPixKeyType() != null ? a.getPixKeyType().name() : null,
                a.getPixKey(),
                a.getStatus(),
                a.getCustomCommissionRate(),
                a.getLastLoginAt(),
                a.getApprovedAt(),
                a.getCreatedAt(),
                totalRefs,
                active,
                montante,
                pending
        );
    }

    private String generateUniqueRefCode(String seed) {
        // Tenta primeiro baseado no nome: ex "Marcos Silva" -> "MARCOS" + 3 digitos
        String prefix = (seed == null ? "REF" : seed.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z]", ""));
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        if (prefix.length() < 3) prefix = "REF";

        for (int attempt = 0; attempt < 10; attempt++) {
            String suffix = randomString(REF_CODE_LENGTH - prefix.length());
            String code = (prefix + suffix).substring(0, REF_CODE_LENGTH);
            if (!affiliateRepository.existsByRefCode(code)) return code;
        }
        // Fallback: codigo totalmente aleatorio
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomString(REF_CODE_LENGTH);
            if (!affiliateRepository.existsByRefCode(code)) return code;
        }
        throw new IllegalStateException("Nao foi possivel gerar refCode unico apos 30 tentativas.");
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(REF_CODE_ALPHABET.charAt(RNG.nextInt(REF_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9+]", "");
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private PixKeyType parsePixKeyType(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return PixKeyType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
