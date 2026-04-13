package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.DashboardSummaryResponse;
import br.com.portal.decode_api.dtos.MonitoringSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final MonitoringService monitoringService;

    public DashboardService(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        MonitoringSummaryResponse mon = monitoringService.summary();

        String monitoredStatus;
        if (mon.downCount() > 0) monitoredStatus = "ALERT";
        else if (mon.slowCount() > 0) monitoredStatus = "WARN";
        else if (mon.okCount() > 0) monitoredStatus = "OK";
        else monitoredStatus = "PENDING_SETUP";

        String msg = mon.okCount() + " OK, " + mon.slowCount() + " lento, " + mon.downCount() + " fora";

        return new DashboardSummaryResponse(
                "ONLINE",
                monitoredStatus,
                (int) mon.incidentsOpen(),
                mon.lastCheckAt() == null ? LocalDateTime.now() : mon.lastCheckAt(),
                msg
        );
    }
}
