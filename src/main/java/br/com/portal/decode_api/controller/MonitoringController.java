package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.service.MonitoringService;
import br.com.portal.decode_api.service.SystemMetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final SystemMetricsService systemMetricsService;

    @GetMapping("/summary")
    public MonitoringSummaryResponse summary() {
        return monitoringService.summary();
    }

    // ===== Sites CRUD =====

    @GetMapping("/sites")
    public List<MonitoredSiteResponse> listSites(@RequestParam(value = "q", required = false) String q) {
        return monitoringService.listSites(q);
    }

    @PostMapping("/sites")
    public MonitoredSiteResponse createSite(@Valid @RequestBody MonitoredSiteCreateRequest request) {
        return monitoringService.createSite(request);
    }

    /**
     * Cadastro rápido: só URL (name opcional).
     */
    @PostMapping("/sites/quick")
    public MonitoredSiteResponse quickCreate(@Valid @RequestBody MonitoredSiteQuickCreateRequest request) {
        return monitoringService.quickCreate(request);
    }

    @PutMapping("/sites/{id}")
    public MonitoredSiteResponse updateSite(@PathVariable UUID id, @Valid @RequestBody MonitoredSiteUpdateRequest request) {
        return monitoringService.updateSite(id, request);
    }

    @DeleteMapping("/sites/{id}")
    public void deleteSite(@PathVariable UUID id) {
        monitoringService.deleteSite(id);
    }

    @PostMapping("/sites/{id}/check")
    public MonitoredSiteResponse runCheck(@PathVariable UUID id) {
        return monitoringService.runCheckNow(id);
    }

    // ===== Details: checks + incidents =====

    @GetMapping("/sites/{id}/checks")
    public List<SiteCheckResponse> listChecks(
            @PathVariable UUID id,
            @RequestParam(value = "hours", required = false) Integer hours,
            @RequestParam(value = "days", required = false) Integer days
    ) {
        LocalDateTime since = monitoringService.resolveSince(hours, days, 24, 7);
        return monitoringService.listChecks(id, since);
    }

    @GetMapping("/sites/{id}/incidents")
    public List<IncidentResponse> listIncidents(
            @PathVariable UUID id,
            @RequestParam(value = "days", required = false) Integer days
    ) {
        LocalDateTime since = monitoringService.resolveSince(null, days, 24, 30);
        return monitoringService.listIncidents(id, since);
    }

    // ===== System metrics (Decode API server) =====

    @GetMapping("/system/overview")
    public SystemOverviewResponse systemOverview() {
        return systemMetricsService.overview();
    }

    @GetMapping("/system/samples")
    public List<SystemMetricSampleResponse> systemSamples(
            @RequestParam(value = "hours", required = false) Integer hours,
            @RequestParam(value = "days", required = false) Integer days
    ) {
        LocalDateTime since = monitoringService.resolveSince(hours, days, 24, 7);
        return systemMetricsService.listSamplesSince(since);
    }
}
