package br.com.portal.decode_api.controller.affiliate;

import br.com.portal.decode_api.dtos.affiliate.*;
import br.com.portal.decode_api.entity.AffiliateCommissionEntity;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.UserRepository;
import br.com.portal.decode_api.service.affiliate.AffiliateDashboardService;
import br.com.portal.decode_api.service.affiliate.AffiliatePayoutRunService;
import br.com.portal.decode_api.service.affiliate.AffiliateService;
import br.com.portal.decode_api.service.affiliate.CommissionCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints administrativos para gestao do programa de afiliados.
 * Roles: ADMIN.
 */
@RestController
@RequestMapping("/api/admin/affiliates")
@RequiredArgsConstructor
public class AdminAffiliateController {

    private final AffiliateService affiliateService;
    private final AffiliatePayoutRunService payoutRunService;
    private final CommissionCalculatorService commissionCalculator;
    private final AffiliateCommissionRepository commissionRepository;
    private final AffiliateDashboardService affiliateDashboardService;
    private final UserRepository userRepository;

    // -----------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------
    @GetMapping
    public Page<AffiliateResponse> list(@RequestParam(value = "q", required = false) String q,
                                         @RequestParam(value = "status", required = false) AffiliateStatus status,
                                         Pageable pageable) {
        return affiliateService.list(q, status, pageable);
    }

    @GetMapping("/{id}")
    public AffiliateResponse get(@PathVariable UUID id) {
        return affiliateService.get(id);
    }

    @PostMapping
    public ResponseEntity<AffiliateResponse> create(@Valid @RequestBody AffiliateCreateRequest req,
                                                     @AuthenticationPrincipal UserDetails principal) {
        UserEntity creator = resolveCurrentUser(principal);
        return ResponseEntity.ok(affiliateService.createByAdmin(req, creator));
    }

    @PutMapping("/{id}")
    public AffiliateResponse update(@PathVariable UUID id, @RequestBody AffiliateUpdateRequest req) {
        return affiliateService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        affiliateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    public AffiliateResponse approve(@PathVariable UUID id,
                                      @Valid @RequestBody AffiliateApproveRequest req,
                                      @AuthenticationPrincipal UserDetails principal) {
        UserEntity approver = resolveCurrentUser(principal);
        return affiliateService.approve(id, req, approver);
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        affiliateService.suspend(id, body.getOrDefault("reason", ""));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        affiliateService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Dashboard completo de um afiliado (mesmos KPIs que o afiliado ve no portal).
     * Permite ao admin inspecionar a performance individual sem precisar logar como ele.
     */
    @GetMapping("/{id}/dashboard")
    public AffiliateDashboardResponse dashboard(@PathVariable UUID id) {
        return affiliateDashboardService.build(id);
    }

    // -----------------------------------------------------------------
    // Commissions
    // -----------------------------------------------------------------
    @GetMapping("/{id}/commissions")
    public Page<AffiliateCommissionResponse> listCommissions(@PathVariable UUID id, Pageable pageable) {
        return commissionRepository.findByAffiliateIdOrderByReferenceMonthDesc(id, pageable)
                .map(this::toCommissionResponse);
    }

    @GetMapping("/{id}/commissions/estimate")
    public Map<String, Object> estimate(@PathVariable UUID id) {
        return Map.of(
                "affiliateId", id,
                "currentMonth", YearMonth.now().toString(),
                "estimateAmount", commissionCalculator.estimateCurrentMonth(id)
        );
    }

    @PostMapping("/commissions/generate")
    public Map<String, Object> generateForMonth(@RequestParam("month") String month) {
        YearMonth ym = YearMonth.parse(month);
        int created = commissionCalculator.generateCommissionsForMonth(ym);
        return Map.of("month", ym.toString(), "created", created);
    }

    @PostMapping("/commissions/{commissionId}/mark-paid")
    public ResponseEntity<Void> markCommissionPaid(@PathVariable UUID commissionId,
                                                    @Valid @RequestBody AffiliateMarkPaidRequest req) {
        payoutRunService.markCommissionPaid(commissionId, req);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------
    // Payout Runs
    // -----------------------------------------------------------------
    @GetMapping("/payout-runs")
    public Page<AffiliatePayoutRunResponse> listPayoutRuns(Pageable pageable) {
        return payoutRunService.list(pageable);
    }

    @GetMapping("/payout-runs/{runId}")
    public AffiliatePayoutRunResponse getPayoutRun(@PathVariable UUID runId) {
        return payoutRunService.get(runId);
    }

    @GetMapping("/payout-runs/{runId}/commissions")
    public List<AffiliateCommissionResponse> listRunCommissions(@PathVariable UUID runId) {
        return commissionRepository.findByPayoutRunId(runId).stream()
                .map(this::toCommissionResponse)
                .toList();
    }

    @PostMapping("/payout-runs")
    public AffiliatePayoutRunResponse createPayoutRun(@RequestParam("month") String month) {
        LocalDate ref = YearMonth.parse(month).atDay(1);
        return payoutRunService.createDraft(ref);
    }

    @PostMapping("/payout-runs/{runId}/review")
    public AffiliatePayoutRunResponse reviewPayoutRun(@PathVariable UUID runId,
                                                       @AuthenticationPrincipal UserDetails principal) {
        return payoutRunService.markReviewed(runId, resolveCurrentUser(principal));
    }

    @PostMapping("/payout-runs/{runId}/execute")
    public AffiliatePayoutRunResponse executePayoutRun(@PathVariable UUID runId) {
        return payoutRunService.markExecuting(runId);
    }

    @PostMapping("/payout-runs/{runId}/cancel")
    public AffiliatePayoutRunResponse cancelPayoutRun(@PathVariable UUID runId,
                                                       @RequestBody Map<String, String> body) {
        return payoutRunService.cancel(runId, body.getOrDefault("reason", ""));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------
    private UserEntity resolveCurrentUser(UserDetails principal) {
        return userRepository.findByEmailIgnoreCaseAndActiveTrue(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado no banco."));
    }

    private AffiliateCommissionResponse toCommissionResponse(AffiliateCommissionEntity c) {
        return new AffiliateCommissionResponse(
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
                c.getNotes()
        );
    }
}
