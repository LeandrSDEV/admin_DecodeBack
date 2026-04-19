package br.com.portal.decode_api.controller.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.dtos.affiliate.*;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.security.affiliate.AffiliatePrincipal;
import br.com.portal.decode_api.service.affiliate.AffiliateAuthService;
import br.com.portal.decode_api.service.affiliate.AffiliateDashboardService;
import br.com.portal.decode_api.service.affiliate.AffiliateDecodeSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    private final AffiliateCommissionRepository commissionRepository;
    private final AffiliateDashboardService dashboardService;
    private final AffiliateDecodeSubmissionService submissionService;
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
        // HashMap (e não Map.of) porque campos opcionais como pixKeyType/pixKey/whatsapp
        // podem ser null — Map.of lança NullPointerException com valores null.
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", a.getId());
        body.put("name", a.getName());
        body.put("email", a.getEmail());
        body.put("refCode", a.getRefCode());
        body.put("whatsapp", a.getWhatsapp());
        body.put("status", a.getStatus());
        body.put("mustChangePassword", Boolean.TRUE.equals(a.getMustChangePw()));
        body.put("pixKeyType", a.getPixKeyType());
        body.put("pixKey", a.getPixKey());
        body.put("commissionRate", a.getCustomCommissionRate() != null
                ? a.getCustomCommissionRate() : props.getBaseRate());
        return body;
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
        return dashboardService.build(a.getId());
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
    // Submissoes de estabelecimento (fechamentos do afiliado)
    // -----------------------------------------------------------------
    @PostMapping("/decode-submissions")
    public AffiliateDecodeSubmissionResponse submitEstablishment(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @Valid @RequestBody AffiliateDecodeSubmissionRequest req) {
        AffiliateEntity a = requireAffiliate(principal);
        return submissionService.submit(a.getId(), req);
    }

    @GetMapping("/decode-submissions")
    public Page<AffiliateDecodeSubmissionResponse> listMySubmissions(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            Pageable pageable) {
        AffiliateEntity a = requireAffiliate(principal);
        return submissionService.listMine(a.getId(), pageable);
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
