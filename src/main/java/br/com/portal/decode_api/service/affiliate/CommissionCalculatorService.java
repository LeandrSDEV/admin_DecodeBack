package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.entity.AffiliateCommissionEntity;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.entity.SubscriptionEntity;
import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
import br.com.portal.decode_api.enums.SubscriptionStatus;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.AffiliateReferralRepository;
import br.com.portal.decode_api.repository.DecodeRepository;
import br.com.portal.decode_api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de calculo de comissoes dos afiliados.
 *
 * Abordagem hibrida:
 * - Job mensal persiste as comissoes oficiais do mes anterior (fonte de verdade).
 * - Dashboards podem chamar estimateCurrentMonth() para ver a projecao em tempo real
 *   do mes que ainda esta em andamento, sem persistir nada.
 *
 * Regras de negocio:
 * - Taxa base = 15% (app.affiliate.base-rate)
 * - Taxa bonus = 18% se o afiliado converteu >= 5 novos clientes no mes (bonus-threshold)
 * - custom_commission_rate do afiliado tem prioridade sobre as duas
 * - Carencia: comissao gerada fica PENDING por N meses (default 2) antes de virar APPROVED
 */
@Service
@RequiredArgsConstructor
public class CommissionCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(CommissionCalculatorService.class);

    private final AffiliateProperties props;
    private final DecodeRepository decodeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AffiliateCommissionRepository commissionRepository;
    private final AffiliateReferralRepository referralRepository;

    // =================================================================
    // JOB MENSAL - roda dia 1 de cada mes as 03:00
    // =================================================================
    @Scheduled(cron = "0 0 3 1 * *")
    public void monthlyCommissionGenerationJob() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("Iniciando geracao mensal de comissoes para {}", previousMonth);
        int created = generateCommissionsForMonth(previousMonth);
        log.info("Geracao mensal concluida: {} comissoes criadas para {}", created, previousMonth);
    }

    // =================================================================
    // JOB DIARIO - libera comissoes que passaram da carencia
    // =================================================================
    @Scheduled(cron = "0 0 4 * * *")
    public void dailyApprovalJob() {
        LocalDate today = LocalDate.now();
        List<AffiliateCommissionEntity> ready = commissionRepository
                .findByStatusAndCarenciaUntilLessThanEqual(AffiliateCommissionStatus.PENDING, today);

        if (ready.isEmpty()) return;

        int approved = 0;
        for (AffiliateCommissionEntity c : ready) {
            // Se o cliente cancelou antes do fim da carencia, estorna
            if (isSubscriptionCancelled(c)) {
                c.setStatus(AffiliateCommissionStatus.REVERSED);
                c.setReversedAt(LocalDateTime.now());
                c.setReverseReason("Cliente cancelou durante a carencia");
            } else {
                c.setStatus(AffiliateCommissionStatus.APPROVED);
                approved++;
            }
            commissionRepository.save(c);
        }
        log.info("dailyApprovalJob: {} comissoes aprovadas, {} estornadas", approved, ready.size() - approved);
    }

    // =================================================================
    // GERACAO DE COMISSOES PARA UM MES
    // Pode ser chamado manualmente pelo admin via endpoint.
    // =================================================================
    @Transactional
    public int generateCommissionsForMonth(YearMonth month) {
        LocalDate referenceMonth = month.atDay(1);
        LocalDate carenciaUntil = referenceMonth.plusMonths(props.getCarenciaMonths() + 1).minusDays(1);
        // ex: referencia 2026-03, carencia 2 meses -> aprovacao em 2026-05-31

        LocalDateTime startOfMonth = referenceMonth.atStartOfDay();
        LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

        List<DecodeEntity> decodesWithAffiliate = decodeRepository.findAllWithAffiliate();

        int created = 0;
        for (DecodeEntity decode : decodesWithAffiliate) {
            AffiliateEntity affiliate = getAffiliate(decode);

            // Ja existe comissao para esse mes? Pula (idempotente)
            Optional<AffiliateCommissionEntity> existing = commissionRepository
                    .findByAffiliateIdAndDecodeIdAndReferenceMonth(
                            affiliate.getId(), decode.getId(), referenceMonth);
            if (existing.isPresent()) continue;

            // Busca a assinatura ativa durante o mes
            Optional<SubscriptionEntity> activeSub = findActiveSubscriptionInMonth(decode, startOfMonth, endOfMonth);
            if (activeSub.isEmpty()) {
                log.debug("Decode {} sem assinatura ativa no mes {}, sem comissao", decode.getId(), month);
                continue;
            }

            SubscriptionEntity sub = activeSub.get();
            BigDecimal rate = resolveRate(affiliate, startOfMonth, endOfMonth);
            BigDecimal amount = sub.getPrice()
                    .multiply(rate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            AffiliateCommissionEntity commission = AffiliateCommissionEntity.builder()
                    .affiliate(affiliate)
                    .decode(decode)
                    .subscription(sub)
                    .referenceMonth(referenceMonth)
                    .planName(sub.getPlanName())
                    .planPrice(sub.getPrice())
                    .commissionRate(rate)
                    .commissionAmount(amount)
                    .status(AffiliateCommissionStatus.PENDING)
                    .carenciaUntil(carenciaUntil)
                    .build();

            commissionRepository.save(commission);
            created++;
            log.debug("Comissao criada: afiliado={}, decode={}, taxa={}, valor={}",
                    affiliate.getRefCode(), decode.getCode(), rate, amount);
        }

        return created;
    }

    // =================================================================
    // ESTIMATIVA EM TEMPO REAL DO MES CORRENTE (hibrido - nao persiste)
    // =================================================================
    @Transactional(readOnly = true)
    public BigDecimal estimateCurrentMonth(UUID affiliateId) {
        YearMonth current = YearMonth.now();
        LocalDateTime startOfMonth = current.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = current.atEndOfMonth().atTime(23, 59, 59);

        List<DecodeEntity> decodes = decodeRepository.findByAffiliateId(affiliateId);
        AffiliateEntity affiliate = decodes.isEmpty() ? null : decodes.get(0).getAffiliate();
        BigDecimal rate = resolveRate(affiliate, startOfMonth, endOfMonth);

        BigDecimal total = BigDecimal.ZERO;
        for (DecodeEntity d : decodes) {
            Optional<SubscriptionEntity> sub = findActiveSubscriptionInMonth(d, startOfMonth, endOfMonth);
            if (sub.isPresent()) {
                BigDecimal amount = sub.get().getPrice()
                        .multiply(rate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                total = total.add(amount);
            }
        }
        return total;
    }

    // =================================================================
    // Helpers
    // =================================================================
    private BigDecimal resolveRate(AffiliateEntity affiliate, LocalDateTime from, LocalDateTime to) {
        if (affiliate != null && affiliate.getCustomCommissionRate() != null) {
            return affiliate.getCustomCommissionRate();
        }
        if (affiliate != null) {
            long conversionsInPeriod = referralRepository.countNewConversionsInPeriod(affiliate.getId(), from, to);
            if (conversionsInPeriod >= props.getBonusThreshold()) {
                return props.getBonusRate();
            }
        }
        return props.getBaseRate();
    }

    private Optional<SubscriptionEntity> findActiveSubscriptionInMonth(DecodeEntity decode,
                                                                        LocalDateTime startOfMonth,
                                                                        LocalDateTime endOfMonth) {
        return subscriptionRepository
                .findFirstByDecodeIdAndStatusOrderByStartedAtDesc(decode.getId(), SubscriptionStatus.ACTIVE)
                .or(() -> subscriptionRepository.findByDecodeIdOrderByStartedAtDesc(decode.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 1)).getContent().stream().findFirst())
                .filter(s -> s.getStartedAt().isBefore(endOfMonth)
                        && (s.getExpiresAt() == null || s.getExpiresAt().isAfter(startOfMonth))
                        && (s.getCancelledAt() == null || s.getCancelledAt().isAfter(startOfMonth))
                );
    }

    private boolean isSubscriptionCancelled(AffiliateCommissionEntity c) {
        if (c.getSubscription() == null) return false;
        return c.getSubscription().getStatus() == SubscriptionStatus.CANCELLED;
    }

    private AffiliateEntity getAffiliate(DecodeEntity decode) {
        return decode.getAffiliate();
    }
}
