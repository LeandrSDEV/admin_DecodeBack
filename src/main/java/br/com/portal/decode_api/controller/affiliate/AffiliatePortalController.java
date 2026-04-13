package br.com.portal.decode_api.controller.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.dtos.affiliate.*;
import br.com.portal.decode_api.entity.AffiliateCommissionEntity;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.AffiliateReferralRepository;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.security.affiliate.AffiliatePrincipal;
import br.com.portal.decode_api.service.affiliate.AffiliateAuthService;
import br.com.portal.decode_api.service.affiliate.CommissionCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Portal do afiliado (self-service).
 *
 * Endpoints sob /api/affiliate/** usam JWT isolado (AffiliateJwtService)
 * e ficam em uma SecurityFilterChain dedicada. Login nao requer autenticacao;
 * os demais exigem ROLE_AFFILIATE e o AffiliatePrincipal no contexto.
 */
@RestController
@RequestMapping("/api/affiliate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AffiliatePortalController {

    private final AffiliateAuthService authService;
    private final AffiliateRepository affiliateRepository;
    private final AffiliateReferralRepository referralRepository;
    private final AffiliateCommissionRepository commissionRepository;
    private final CommissionCalculatorService commissionCalculator;
    private final AffiliateProperties props;

    // -----------------------------------------------------------------
    // Auth (publico dentro dessa chain)
    // -----------------------------------------------------------------
    @PostMapping("/auth/login")
    public ResponseEntity<AffiliateLoginResponse> login(@Valid @RequestBody AffiliateLoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (AffiliateAuthService.BadCredentialsException e) {
            return ResponseEntity.status(401).body(null);
        }
    }

    // -----------------------------------------------------------------
    // Me (perfil do afiliado logado)
    // -----------------------------------------------------------------
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AffiliatePrincipal principal) {
        AffiliateEntity a = requireAffiliate(principal);
        return Map.of(
                "id", a.getId(),
                "name", a.getName(),
                "email", a.getEmail(),
                "refCode", a.getRefCode(),
                "whatsapp", a.getWhatsapp(),
                "status", a.getStatus(),
                "mustChangePassword", Boolean.TRUE.equals(a.getMustChangePw()),
                "pixKeyType", a.getPixKeyType(),
                "pixKey", a.getPixKey(),
                "commissionRate", a.getCustomCommissionRate() != null
                        ? a.getCustomCommissionRate() : props.getBaseRate()
        );
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AffiliatePrincipal principal,
                                                @RequestBody Map<String, String> body) {
        AffiliateEntity a = requireAffiliate(principal);
        authService.changePassword(a, body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------
    // Dashboard (principal endpoint do portal)
    // -----------------------------------------------------------------
    @GetMapping("/dashboard")
    public AffiliateDashboardResponse dashboard(@AuthenticationPrincipal AffiliatePrincipal principal) {
        AffiliateEntity a = requireAffiliate(principal);

        int activeClients = (int) referralRepository.countByAffiliateIdAndStatus(
                a.getId(), br.com.portal.decode_api.enums.AffiliateReferralStatus.CONVERTED);
        int totalConversions = activeClients
                + (int) referralRepository.countByAffiliateIdAndStatus(
                        a.getId(), br.com.portal.decode_api.enums.AffiliateReferralStatus.CHURNED);

        BigDecimal paid = commissionRepository.sumAmountByAffiliateAndStatus(a.getId(), AffiliateCommissionStatus.PAID);
        BigDecimal approved = commissionRepository.sumAmountByAffiliateAndStatus(a.getId(), AffiliateCommissionStatus.APPROVED);
        BigDecimal pending = commissionRepository.sumAmountByAffiliateAndStatus(a.getId(), AffiliateCommissionStatus.PENDING);
        BigDecimal lifetime = paid.add(approved).add(pending);

        BigDecimal currentEstimate = commissionCalculator.estimateCurrentMonth(a.getId());
        BigDecimal lastMonth = commissionRepository.sumAmountByAffiliateAndMonth(
                a.getId(), YearMonth.now().minusMonths(1).atDay(1));

        BigDecimal rate = a.getCustomCommissionRate() != null
                ? a.getCustomCommissionRate() : props.getBaseRate();

        String shareLink = props.getLandingBaseUrl() + "/?ref=" + a.getRefCode();

        List<AffiliateDashboardResponse.MonthlyBreakdown> lastSix = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate month = YearMonth.now().minusMonths(i).atDay(1);
            BigDecimal monthAmount = commissionRepository.sumAmountByAffiliateAndMonth(a.getId(), month);
            List<AffiliateCommissionEntity> monthCommissions = commissionRepository
                    .findByAffiliateIdAndReferenceMonth(a.getId(), month);
            String status = monthCommissions.isEmpty() ? "NENHUMA" :
                    monthCommissions.stream().anyMatch(c -> c.getStatus() == AffiliateCommissionStatus.PAID) ? "PAGO" :
                    monthCommissions.stream().anyMatch(c -> c.getStatus() == AffiliateCommissionStatus.APPROVED) ? "APROVADO" :
                    "PENDENTE";
            lastSix.add(new AffiliateDashboardResponse.MonthlyBreakdown(
                    month, monthCommissions.size(), monthAmount, status));
        }

        return new AffiliateDashboardResponse(
                a.getRefCode(),
                shareLink,
                rate,
                activeClients,
                totalConversions,
                lifetime,
                currentEstimate,
                lastMonth,
                pending,
                approved,
                paid,
                YearMonth.now().plusMonths(1).atDay(10),
                lastSix
        );
    }

    // -----------------------------------------------------------------
    // Historico de comissoes
    // -----------------------------------------------------------------
    @GetMapping("/commissions")
    public Page<AffiliateCommissionResponse> listMyCommissions(@AuthenticationPrincipal AffiliatePrincipal principal,
                                                                Pageable pageable) {
        return commissionRepository.findByAffiliateIdOrderByReferenceMonthDesc(principal.getId(), pageable)
                .map(c -> new AffiliateCommissionResponse(
                        c.getId(),
                        c.getAffiliate().getId(),
                        c.getAffiliate().getName(),
                        c.getDecode().getId(),
                        c.getDecode().getName(),
                        c.getReferenceMonth(),
                        c.getPlanName(),
                        c.getPlanPrice(),
                        c.getCommissionRate(),
                        c.getCommissionAmount(),
                        c.getStatus(),
                        c.getCarenciaUntil(),
                        c.getPayoutRun() != null ? c.getPayoutRun().getId() : null,
                        c.getPaidAt(),
                        c.getPaidReference(),
                        null
                ));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------
    private AffiliateEntity requireAffiliate(AffiliatePrincipal principal) {
        if (principal == null) {
            throw new RuntimeException("Nao autenticado");
        }
        return affiliateRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Afiliado nao encontrado"));
    }
}
