package br.com.portal.decode_api.dtos.affiliate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dashboard do portal do afiliado com mix de dados persistidos + estimativa em tempo real. */
public record AffiliateDashboardResponse(
        String refCode,
        String shareLink,                     // link pronto pra copiar com o ref
        BigDecimal commissionRate,            // taxa atual aplicada ao afiliado
        int activeClients,                    // clientes pagantes agora
        int totalConversions,                 // total historico
        BigDecimal lifetimeEarned,            // total ja pago + aprovado + pendente
        BigDecimal currentMonthEstimate,      // estimativa do mes corrente (C)
        BigDecimal lastMonthEarned,           // oficial do mes passado
        BigDecimal pendingCarencia,           // em carencia
        BigDecimal readyForPayout,            // aprovado, aguardando payout run
        BigDecimal alreadyPaid,               // ja recebido
        LocalDate nextPayoutDate,             // proxima data estimada de pagamento
        List<MonthlyBreakdown> lastSixMonths
) {
    public record MonthlyBreakdown(
            LocalDate month,
            int clientCount,
            BigDecimal commissionAmount,
            String status
    ) {}
}
