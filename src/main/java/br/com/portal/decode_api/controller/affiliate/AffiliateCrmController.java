package br.com.portal.decode_api.controller.affiliate;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.security.affiliate.AffiliatePrincipal;
import br.com.portal.decode_api.service.affiliate.AffiliateCrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * CRUD de Leads e Interações exposto ao afiliado autenticado.
 * Cada operação é restrita aos recursos do próprio afiliado pelo
 * AffiliateCrmService.
 */
@RestController
@RequestMapping("/api/affiliate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AffiliateCrmController {

    private final AffiliateCrmService crmService;

    // -----------------------------------------------------------------
    // STATS (KPIs do dashboard pessoal)
    // -----------------------------------------------------------------
    @GetMapping("/stats")
    public Map<String, Object> stats(@AuthenticationPrincipal AffiliatePrincipal principal) {
        return crmService.stats(principal.getId());
    }

    // -----------------------------------------------------------------
    // LEADS
    // -----------------------------------------------------------------
    @GetMapping("/leads")
    public Page<LeadResponse> listLeads(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return crmService.listLeads(principal.getId(), q, pageable);
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse createLead(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @Valid @RequestBody LeadCreateRequest req
    ) {
        return crmService.createLead(principal.getId(), req);
    }

    @PutMapping("/leads/{id}")
    public LeadResponse updateLead(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody LeadUpdateRequest req
    ) {
        return crmService.updateLead(principal.getId(), id, req);
    }

    @DeleteMapping("/leads/{id}")
    public ResponseEntity<Void> deleteLead(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @PathVariable UUID id
    ) {
        crmService.deleteLead(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------
    // INTERACTIONS
    // -----------------------------------------------------------------
    @GetMapping("/interactions")
    public Page<InteractionResponse> listInteractions(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return crmService.listInteractions(principal.getId(), q, pageable);
    }

    @PostMapping("/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionResponse createInteraction(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @Valid @RequestBody InteractionCreateRequest req
    ) {
        return crmService.createInteraction(principal.getId(), req);
    }

    @PutMapping("/interactions/{id}")
    public InteractionResponse updateInteraction(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody InteractionUpdateRequest req
    ) {
        return crmService.updateInteraction(principal.getId(), id, req);
    }

    @DeleteMapping("/interactions/{id}")
    public ResponseEntity<Void> deleteInteraction(
            @AuthenticationPrincipal AffiliatePrincipal principal,
            @PathVariable UUID id
    ) {
        crmService.deleteInteraction(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
