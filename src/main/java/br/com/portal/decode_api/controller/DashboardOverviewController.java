package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.repository.DecodeRepository;
import br.com.portal.decode_api.service.tenant.MetricsProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint do painel admin que entrega TUDO que o dashboard precisa em
 * uma única chamada, agregando:
 *
 *   - Lista de tenants com métricas operacionais (vendas, pedidos, usuários)
 *     vinda do lanchonete via S2S
 *   - Top 3 afiliados (por comissão total ganha + clientes ativos)
 *   - Totais do CRM: contagem de afiliados ativos, total de decodes
 *
 * Auth: ADMIN do CRM (JWT). Frontend chama 1x e renderiza tudo.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardOverviewController {

    private final MetricsProxyService metricsProxy;
    private final AffiliateRepository affiliateRepository;
    private final DecodeRepository decodeRepository;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> tenancy = metricsProxy.aggregate();

        long affiliatesActive = affiliateRepository.countByStatus(AffiliateStatus.ACTIVE);
        long affiliatesPending = affiliateRepository.countByStatus(AffiliateStatus.PENDING);
        long decodesTotal = decodeRepository.count();

        List<Map<String, Object>> top3 = topAffiliates(3);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenancy", tenancy);
        response.put("crm", Map.of(
                "affiliatesActive", affiliatesActive,
                "affiliatesPending", affiliatesPending,
                "decodesTotal", decodesTotal
        ));
        response.put("topAffiliates", top3);
        return response;
    }

    private List<Map<String, Object>> topAffiliates(int n) {
        List<Object[]> rows = affiliateRepository.findTopBySalesAndConversions(PageRequest.of(0, n));
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("id", ((UUID) row[0]).toString());
            item.put("name", row[1]);
            item.put("refCode", row[2]);
            item.put("activeClients", ((Number) row[3]).longValue());
            BigDecimal earned = row[4] == null ? BigDecimal.ZERO : (BigDecimal) row[4];
            item.put("totalEarned", earned);
            result.add(item);
        }
        return result;
    }
}
