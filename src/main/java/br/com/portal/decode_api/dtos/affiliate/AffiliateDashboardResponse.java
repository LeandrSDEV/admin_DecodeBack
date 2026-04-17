package br.com.portal.decode_api.dtos.affiliate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard do portal do afiliado: visao completa com estabelecimentos (decodes),
 * comissoes (diario/mensal/total), situacao de repasses e serie temporal para
 * grafico de alta/baixa.
 */
public record AffiliateDashboardResponse(
        String refCode,
        String shareLink,
        BigDecimal commissionRate,

        // Estabelecimentos (decodes) que o afiliado trouxe
        int decodesToday,
        int decodesThisMonth,
        int decodesTotal,
        int activeClients,
        int totalConversions,

        // Comissao (valor)
        BigDecimal dailyEarned,            // gerado hoje (comissoes com referenceMonth atual + decodes attached hoje)
        BigDecimal currentMonthEstimate,   // estimativa em tempo real do mes corrente
        BigDecimal lastMonthEarned,
        BigDecimal lifetimeEarned,

        // Repasses
        BigDecimal pendingCarencia,        // segurando na carencia
        BigDecimal readyForPayout,         // aprovado, falta pagar
        BigDecimal alreadyPaid,            // ja repassado
        LocalDate nextPayoutDate,

        // Serie temporal para grafico alta/baixa (ultimos 30 dias)
        List<DailyProduction> productionTrend,

        // Breakdown mensal (6 meses)
        List<MonthlyBreakdown> lastSixMonths
) {
    public record DailyProduction(
            LocalDate date,
            int decodes,
            BigDecimal commissionAmount
    ) {}

    public record MonthlyBreakdown(
            LocalDate month,
            int clientCount,
            BigDecimal commissionAmount,
            String status
    ) {}
}
