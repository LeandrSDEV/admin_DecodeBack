package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.entity.AffiliateReferralEntity;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.entity.LeadEntity;
import br.com.portal.decode_api.enums.AffiliateReferralStatus;
import br.com.portal.decode_api.repository.AffiliateReferralRepository;
import br.com.portal.decode_api.repository.AffiliateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Gerencia a atribuicao (attribution) afiliado -> lead -> decode.
 *
 * Fluxo tipico:
 * 1) Cliente clica no link com ?ref=XXX -> trackClick()
 * 2) Cliente chega no WhatsApp -> admin cadastra LeadEntity -> attachLead()
 * 3) Lead vira cliente pagante -> admin vincula DecodeEntity -> attachDecode()
 * 4) Cliente cancela -> markChurned()
 */
@Service
@RequiredArgsConstructor
public class AffiliateReferralService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateReferralService.class);

    private final AffiliateRepository affiliateRepository;
    private final AffiliateReferralRepository referralRepository;

    @Transactional
    public Optional<AffiliateReferralEntity> trackClick(String refCode,
                                                        String sourceIp,
                                                        String userAgent,
                                                        String landingUrl) {
        if (refCode == null || refCode.isBlank()) return Optional.empty();

        Optional<AffiliateEntity> opt = affiliateRepository.findByRefCode(refCode.trim().toUpperCase());
        if (opt.isEmpty()) {
            log.debug("Click com refCode desconhecido: {}", refCode);
            return Optional.empty();
        }
        AffiliateEntity a = opt.get();
        if (!a.isActive()) {
            log.debug("Click em refCode inativo: {} (status={})", refCode, a.getStatus());
            return Optional.empty();
        }

        AffiliateReferralEntity ref = AffiliateReferralEntity.builder()
                .affiliate(a)
                .firstTouchAt(LocalDateTime.now())
                .sourceIp(sourceIp)
                .userAgent(truncate(userAgent, 500))
                .landingUrl(truncate(landingUrl, 500))
                .status(AffiliateReferralStatus.CLICKED)
                .build();
        return Optional.of(referralRepository.save(ref));
    }

    @Transactional
    public AffiliateReferralEntity attachLead(UUID affiliateId, LeadEntity lead) {
        AffiliateEntity a = affiliateRepository.findById(affiliateId)
                .orElseThrow(() -> new IllegalArgumentException("Afiliado nao encontrado: " + affiliateId));

        // Evita atribuicao duplicada
        Optional<AffiliateReferralEntity> existing = referralRepository.findByLeadId(lead.getId());
        if (existing.isPresent()) {
            log.warn("Lead {} ja possui referral para afiliado {}", lead.getId(), existing.get().getAffiliate().getId());
            return existing.get();
        }

        AffiliateReferralEntity ref = AffiliateReferralEntity.builder()
                .affiliate(a)
                .firstTouchAt(LocalDateTime.now())
                .status(AffiliateReferralStatus.LEAD)
                .lead(lead)
                .build();
        return referralRepository.save(ref);
    }

    /**
     * Promove um lead ja atribuido a um afiliado para conversao (virou cliente pagante).
     * Se o lead nao tinha referral (foi attach direto no decode), cria um novo.
     */
    @Transactional
    public AffiliateReferralEntity markConverted(UUID affiliateId, LeadEntity lead, DecodeEntity decode) {
        AffiliateReferralEntity ref;
        Optional<AffiliateReferralEntity> existing = lead != null
                ? referralRepository.findByLeadId(lead.getId())
                : Optional.empty();

        if (existing.isPresent()) {
            ref = existing.get();
            ref.setStatus(AffiliateReferralStatus.CONVERTED);
            ref.setDecode(decode);
            ref.setConvertedAt(LocalDateTime.now());
        } else {
            AffiliateEntity a = affiliateRepository.findById(affiliateId)
                    .orElseThrow(() -> new IllegalArgumentException("Afiliado nao encontrado: " + affiliateId));
            ref = AffiliateReferralEntity.builder()
                    .affiliate(a)
                    .firstTouchAt(LocalDateTime.now())
                    .status(AffiliateReferralStatus.CONVERTED)
                    .lead(lead)
                    .decode(decode)
                    .convertedAt(LocalDateTime.now())
                    .build();
        }
        AffiliateReferralEntity saved = referralRepository.save(ref);
        log.info("Referral convertido: afiliado={}, decode={}", saved.getAffiliate().getId(), decode.getId());
        return saved;
    }

    @Transactional
    public void markChurned(UUID decodeId) {
        referralRepository.findByDecodeId(decodeId).ifPresent(ref -> {
            ref.setStatus(AffiliateReferralStatus.CHURNED);
            ref.setChurnedAt(LocalDateTime.now());
            referralRepository.save(ref);
            log.info("Referral churned: decode={}, afiliado={}", decodeId, ref.getAffiliate().getId());
        });
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
