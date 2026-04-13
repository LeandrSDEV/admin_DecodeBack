package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.DashboardSummaryResponse;
import br.com.portal.decode_api.dtos.events.KpisResponse;
import br.com.portal.decode_api.service.DashboardService;
import br.com.portal.decode_api.service.events.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EventService eventService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/kpis")
    public KpisResponse kpis(@RequestParam(defaultValue = "5m") String window) {
        return eventService.kpis(window);
    }
}