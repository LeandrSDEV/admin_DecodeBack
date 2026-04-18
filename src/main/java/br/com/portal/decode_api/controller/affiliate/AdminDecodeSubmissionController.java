package br.com.portal.decode_api.controller.affiliate;

import br.com.portal.decode_api.dtos.affiliate.AffiliateDecodeSubmissionRejectRequest;
import br.com.portal.decode_api.dtos.affiliate.AffiliateDecodeSubmissionResponse;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.AffiliateDecodeSubmissionStatus;
import br.com.portal.decode_api.repository.UserRepository;
import br.com.portal.decode_api.service.affiliate.AffiliateDecodeSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints administrativos de revisao das propostas de estabelecimento
 * submetidas pelos afiliados.
 */
@RestController
@RequestMapping("/api/admin/decode-submissions")
@RequiredArgsConstructor
public class AdminDecodeSubmissionController {

    private final AffiliateDecodeSubmissionService submissionService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<AffiliateDecodeSubmissionResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) AffiliateDecodeSubmissionStatus status,
            Pageable pageable) {
        return submissionService.listAll(q, status, pageable);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of("pendingCount", submissionService.countPending());
    }

    @GetMapping("/{id}")
    public AffiliateDecodeSubmissionResponse get(@PathVariable UUID id) {
        return submissionService.get(id);
    }

    @PostMapping("/{id}/approve")
    public AffiliateDecodeSubmissionResponse approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        return submissionService.approve(id, resolveUser(principal));
    }

    @PostMapping("/{id}/reject")
    public AffiliateDecodeSubmissionResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody AffiliateDecodeSubmissionRejectRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return submissionService.reject(id, req.reason(), resolveUser(principal));
    }

    private UserEntity resolveUser(UserDetails principal) {
        return userRepository.findByEmailIgnoreCaseAndActiveTrue(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado no banco."));
    }
}
