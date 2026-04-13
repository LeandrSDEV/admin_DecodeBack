package br.com.portal.decode_api.controller.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.dtos.affiliate.AffiliateApplyRequest;
import br.com.portal.decode_api.dtos.affiliate.AffiliateApplyResponse;
import br.com.portal.decode_api.dtos.affiliate.AffiliateTrackRequest;
import br.com.portal.decode_api.service.affiliate.AffiliateReferralService;
import br.com.portal.decode_api.service.affiliate.AffiliateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints publicos do programa de afiliados.
 * - Recebem o form da landing Lovable em /apply
 * - Rastreiam clicks em /track
 * - Expoem info leve do programa em /info
 *
 * Todos liberados em SecurityConfig como permitAll.
 */
@RestController
@RequestMapping("/api/public/affiliates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicAffiliateController {

    private final AffiliateService affiliateService;
    private final AffiliateReferralService referralService;
    private final AffiliateProperties props;

    @PostMapping("/apply")
    public ResponseEntity<AffiliateApplyResponse> apply(@Valid @RequestBody AffiliateApplyRequest req) {
        return ResponseEntity.ok(affiliateService.apply(req));
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> track(@RequestBody AffiliateTrackRequest req,
                                                      HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        boolean tracked = referralService.trackClick(
                req.refCode(),
                ip,
                req.userAgent(),
                req.landingUrl()
        ).isPresent();
        return ResponseEntity.ok(Map.of("tracked", tracked));
    }

    /**
     * Endpoint GET alternativo para pixel/beacon tracking (permite click nos emails
     * e meta tags quando o front nao pode fazer POST por CORS).
     */
    @GetMapping("/track")
    public ResponseEntity<Map<String, Object>> trackGet(@RequestParam("ref") String refCode,
                                                         HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        String ua = servletRequest.getHeader("User-Agent");
        String referer = servletRequest.getHeader("Referer");
        boolean tracked = referralService.trackClick(refCode, ip, ua, referer).isPresent();
        return ResponseEntity.ok(Map.of("tracked", tracked));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "baseRate", props.getBaseRate(),
                "bonusRate", props.getBonusRate(),
                "bonusThreshold", props.getBonusThreshold(),
                "carenciaMonths", props.getCarenciaMonths(),
                "landingBaseUrl", props.getLandingBaseUrl(),
                "portalBaseUrl", props.getPortalBaseUrl()
        ));
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
