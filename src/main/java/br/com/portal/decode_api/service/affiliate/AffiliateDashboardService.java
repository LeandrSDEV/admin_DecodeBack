package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.dtos.affiliate.AffiliateDashboardResponse;
import br.com.portal.decode_api.entity.AffiliateCommissionEntity;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
import br.com.portal.decode_api.enums.AffiliateReferralStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.AffiliateReferralRepository;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.repository.DecodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Monta o dashboard completo de um afiliado. Extraido do controller para poder
 * ser reutilizado pelo portal do afiliado (self) e pelo admin (filtro por
 * afiliado no dashboard administrativo).
 */
@Service
@RequiredArgsConstructor
public class AffiliateDashboardService {

    private static final int TREND_DAYS = 30;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AffiliateRepository affiliateRepository;
    private final AffiliateReferralRepository referralRepository;
    private final AffiliateCommissionRepository commissionRepository;
    private final DecodeRepository decodeRepository;
    private final CommissionCalculatorService commissionCalculator;
    private final AffiliateProperties props;

    @Transactional(readOnly = true)
    public AffiliateDashboardResponse build(UUID affiliateId) {
        AffiliateEntity a = affiliateRepository.findById(affiliateId)
                .orElseThrow(() -> new EntityNotFoundException("Affiliate", affiliateId));

        BigDecimal rate = a.getCustomCommissionRate() != null
                ? a.getCustomCommissionRate() : props.getBaseRate();

        // -------- Estabelecimentos (decodes) --------
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        int decodesToday = (int) decodeRepository.countByAffiliateIdAndAttachedBetween(
                a.getId(), startOfToday, startOfTomorrow);
        int decodesThisMonth = (int) decodeRepository.countByAffiliateIdAndAttachedBetween(
                a.getId(), startOfMonth, startOfNextMonth);
        int decodesTotal = (int) decodeRepository.countByAffiliateId(a.getId());

        int activeClients = (int) referralRepository.countByAffiliateIdAndStatus(
                a.getId(), AffiliateReferralStatus.CONVERTED);
        int totalConversions = activeClients
                + (int) referralRepository.countByAffiliateIdAndStatus(
                        a.getId(), AffiliateReferralStatus.CHURNED);

        // -------- Comissoes (totais por status) --------
        BigDecimal paid = commissionRepository.sumAmountByAffiliateAndStatus(
                a.getId(), AffiliateCommissionStatus.PAID);
        BigDecimal approved = commissionRepository.sumAmountByAffiliateAndStatus(
                a.getId(), AffiliateCommissionStatus.APPROVED);
        BigDecimal pending = commissionRepository.sumAmountByAffiliateAndStatus(
                a.getId(), AffiliateCommissionStatus.PENDING);
        BigDecimal lifetime = paid.add(approved).add(pending);

        BigDecimal currentEstimate = commissionCalculator.estimateCurrentMonth(a.getId());
        BigDecimal lastMonth = commissionRepository.sumAmountByAffiliateAndMonth(
                a.getId(), YearMonth.now().minusMonths(1).atDay(1));

        // -------- Serie diaria de producao (ultimos 30 dias) + comissao gerada hoje --------
        LocalDateTime trendStart = today.minusDays(TREND_DAYS - 1L).atStartOfDay();
        List<Object[]> rawDaily = decodeRepository.dailyProductionByAffiliateSince(a.getId(), trendStart);

        Map<LocalDate, long[]> dailyCountByDate = new HashMap<>();
        Map<LocalDate, BigDecimal> dailyRevenueByDate = new HashMap<>();
        for (Object[] row : rawDaily) {
            LocalDate day = toLocalDate(row[0]);
            long count = ((Number) row[1]).longValue();
            BigDecimal revenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            dailyCountByDate.put(day, new long[]{count});
            dailyRevenueByDate.put(day, revenue);
        }

        List<AffiliateDashboardResponse.DailyProduction> trend = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long[] count = dailyCountByDate.get(day);
            BigDecimal revenue = dailyRevenueByDate.getOrDefault(day, BigDecimal.ZERO);
            BigDecimal commissionForDay = revenue
                    .multiply(rate)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            trend.add(new AffiliateDashboardResponse.DailyProduction(
                    day,
                    count != null ? (int) count[0] : 0,
                    commissionForDay
            ));
        }

        BigDecimal dailyEarned = BigDecimal.ZERO;
        if (!trend.isEmpty()) {
            dailyEarned = trend.get(trend.size() - 1).commissionAmount();
        }

        // -------- Breakdown mensal (ultimos 6 meses) --------
        List<AffiliateDashboardResponse.MonthlyBreakdown> lastSix = new ArrayList<>(6);
        for (int i = 5; i >= 0; i--) {
            LocalDate month = YearMonth.now().minusMonths(i).atDay(1);
            BigDecimal amount = commissionRepository.sumAmountByAffiliateAndMonth(a.getId(), month);
            List<AffiliateCommissionEntity> monthComms = commissionRepository
                    .findByAffiliateIdAndReferenceMonth(a.getId(), month);
            String status = deriveMonthStatus(monthComms);
            lastSix.add(new AffiliateDashboardResponse.MonthlyBreakdown(
                    month, monthComms.size(), amount, status));
        }

        String shareLink = props.getLandingBaseUrl() + "/?ref=" + a.getRefCode();

        return new AffiliateDashboardResponse(
                a.getRefCode(),
                shareLink,
                rate,
                decodesToday,
                decodesThisMonth,
                decodesTotal,
                activeClients,
                totalConversions,
                dailyEarned,
                currentEstimate,
                lastMonth,
                lifetime,
                pending,
                approved,
                paid,
                YearMonth.now().plusMonths(1).atDay(10),
                trend,
                lastSix
        );
    }

    private String deriveMonthStatus(List<AffiliateCommissionEntity> monthComms) {
        if (monthComms.isEmpty()) return "NENHUMA";
        if (monthComms.stream().anyMatch(c -> c.getStatus() == AffiliateCommissionStatus.PAID)) return "PAGO";
        if (monthComms.stream().anyMatch(c -> c.getStatus() == AffiliateCommissionStatus.APPROVED)) return "APROVADO";
        return "PENDENTE";
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate();
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
        // fallback: try to parse string
        return LocalDate.parse(value.toString());
    }
}
