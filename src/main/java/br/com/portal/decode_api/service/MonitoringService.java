package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.entity.*;
import br.com.portal.decode_api.enums.IncidentStatus;
import br.com.portal.decode_api.enums.SiteStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final long MAX_BYTES = 1_000_000; // 1MB safety

    private final MonitoredSiteRepository siteRepository;
    private final SiteCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;
    private final PushoverService pushoverService;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Scheduled(fixedDelayString = "${app.monitoring.poll-interval-ms:60000}")
    public void pollAllEnabledSites() {
        // best-effort: erros em um site não derrubam o job
        for (MonitoredSiteEntity s : siteRepository.findByEnabledTrue()) {
            try {
                runCheck(s.getId());
            } catch (Exception ignored) {
                // silencioso: check já registra DOWN se necessário
            }
        }
    }

    public LocalDateTime resolveSince(Integer hours, Integer days, int defaultHours, int defaultDays) {
        int h = hours != null ? hours : (days == null ? defaultHours : 0);
        int d = days != null ? days : (hours == null ? defaultDays : 0);

        LocalDateTime now = LocalDateTime.now();
        if (d > 0) return now.minusDays(d);
        return now.minusHours(Math.max(1, h));
    }

    @Transactional(readOnly = true)
    public List<MonitoredSiteResponse> listSites(String q) {
        LocalDateTime since30d = LocalDateTime.now().minusDays(30);

        return siteRepository.search(q).stream()
                .map(site -> toResponse(site, since30d))
                .toList();
    }

    @Transactional
    public MonitoredSiteResponse createSite(MonitoredSiteCreateRequest req) {
        MonitoredSiteEntity entity = MonitoredSiteEntity.builder()
                .name(req.name().trim())
                .url(req.url().trim())
                .enabled(req.enabled() == null ? Boolean.TRUE : req.enabled())
                .slowThresholdMs(req.slowThresholdMs())
                .timeoutMs(req.timeoutMs())
                .build();

        MonitoredSiteEntity saved = siteRepository.save(entity);
        // dispara 1 check inicial para já aparecer no painel
        try { runCheck(saved.getId()); } catch (Exception ignored) {}

        return toResponse(saved, LocalDateTime.now().minusDays(30));
    }

    @Transactional
    public MonitoredSiteResponse quickCreate(MonitoredSiteQuickCreateRequest req) {
        String url = req.url().trim();
        String name = (req.name() == null || req.name().isBlank())
                ? guessNameFromUrl(url)
                : req.name().trim();

        MonitoredSiteEntity entity = MonitoredSiteEntity.builder()
                .name(name)
                .url(url)
                .enabled(Boolean.TRUE)
                .slowThresholdMs(600)
                .timeoutMs(5000)
                .build();

        MonitoredSiteEntity saved = siteRepository.save(entity);
        try { runCheck(saved.getId()); } catch (Exception ignored) {}

        return toResponse(saved, LocalDateTime.now().minusDays(30));
    }

    @Transactional
    public MonitoredSiteResponse updateSite(UUID id, MonitoredSiteUpdateRequest req) {
        MonitoredSiteEntity entity = siteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site não encontrado"));

        if (req.name() != null && !req.name().isBlank()) entity.setName(req.name().trim());
        if (req.url() != null && !req.url().isBlank()) entity.setUrl(req.url().trim());
        if (req.enabled() != null) entity.setEnabled(req.enabled());
        if (req.slowThresholdMs() != null) entity.setSlowThresholdMs(req.slowThresholdMs());
        if (req.timeoutMs() != null) entity.setTimeoutMs(req.timeoutMs());

        MonitoredSiteEntity saved = siteRepository.save(entity);
        return toResponse(saved, LocalDateTime.now().minusDays(30));
    }

    @Transactional
    public void deleteSite(UUID id) {
        if (!siteRepository.existsById(id)) throw new EntityNotFoundException("Site não encontrado");
        siteRepository.deleteById(id);
        log.info("Site monitorado deletado: id={}", id);
    }

    @Transactional
    public MonitoredSiteResponse runCheckNow(UUID siteId) {
        runCheck(siteId);
        MonitoredSiteEntity site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site não encontrado"));
        return toResponse(site, LocalDateTime.now().minusDays(30));
    }

    public List<SiteCheckResponse> listChecks(UUID siteId, LocalDateTime since) {
        return checkRepository.listBySiteSince(siteId, since).stream()
                .map(this::toCheckResponse)
                .toList();
    }

    public List<IncidentResponse> listIncidents(UUID siteId, LocalDateTime since) {
        return incidentRepository.listBySiteSince(siteId, since).stream()
                .map(i -> new IncidentResponse(
                        i.getId(),
                        i.getStatus(),
                        i.getOpenedAt(),
                        i.getClosedAt(),
                        i.getMessage(),
                        i.getLastCheck() == null ? null : i.getLastCheck().getId()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitoringSummaryResponse summary() {
        List<MonitoredSiteResponse> sites = listSites(null);
        int ok = (int) sites.stream().filter(s -> s.status() == SiteStatus.OK).count();
        int slow = (int) sites.stream().filter(s -> s.status() == SiteStatus.SLOW).count();
        int down = (int) sites.stream().filter(s -> s.status() == SiteStatus.DOWN).count();
        long open = incidentRepository.countByStatus(IncidentStatus.OPEN);

        LocalDateTime last = sites.stream()
                .map(MonitoredSiteResponse::lastCheckAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new MonitoringSummaryResponse(ok, slow, down, open, last);
    }

    /**
     * Executa um check HTTP e registra em site_checks + abre/fecha incidentes.
     * Também tenta coletar detalhes (DNS/TLS/TTFB/SSL) e autodetectar endpoints de métricas (Prometheus/Actuator).
     */
    public void runCheck(UUID siteId) {
        MonitoredSiteEntity site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site não encontrado"));

        String originalUrl = site.getUrl();

        SiteStatus status = SiteStatus.DOWN;
        Integer rtt = 0;
        Integer httpStatus = null;
        String err = null;

        Integer dnsMs = null;
        Integer tlsMs = null;
        Integer ttfbMs = null;
        Long bytesRead = null;
        String resolvedIp = null;
        String finalUrl = null;
        Integer redirectsCount = null;
        LocalDateTime sslValidTo = null;
        Integer sslDaysLeft = null;
        String metricsEndpoint = null;
        String metricsJson = null;

        long startAll = System.nanoTime();

        try {
            URI uri = URI.create(originalUrl);

            // DNS
            long dnsStart = System.nanoTime();
            resolvedIp = resolveIp(uri.getHost());
            long dnsEnd = System.nanoTime();
            dnsMs = toMs(dnsEnd - dnsStart);

            // TLS certificate info (best-effort)
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                SSLInfo sslInfo = fetchSslInfo(uri);
                if (sslInfo != null) {
                    tlsMs = sslInfo.tlsMs;
                    sslValidTo = sslInfo.validTo;
                    sslDaysLeft = sslInfo.daysLeft;
                }
            }

            // HTTP request
            long start = System.nanoTime();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(site.getTimeoutMs() == null ? 5000 : site.getTimeoutMs()))
                    .GET()
                    .header("User-Agent", "Decode-Monitor/1.0")
                    .header("Accept", "*/*")
                    .build();

            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            long headersAt = System.nanoTime();

            httpStatus = response.statusCode();
            finalUrl = response.uri() == null ? originalUrl : response.uri().toString();
            redirectsCount = (finalUrl != null && !finalUrl.equals(originalUrl)) ? 1 : 0;

            rtt = toMs(headersAt - start);

            // TTFB + bytes
            long firstByteAt = headersAt;
            long totalRead = 0;
            try (InputStream in = response.body()) {
                byte[] buf = new byte[8192];
                int n;
                boolean gotFirstByte = false;
                while ((n = in.read(buf)) != -1) {
                    if (!gotFirstByte) {
                        firstByteAt = System.nanoTime();
                        gotFirstByte = true;
                    }
                    totalRead += n;
                    if (totalRead >= MAX_BYTES) break;
                }
            } catch (Exception ignored) {}

            bytesRead = totalRead;
            ttfbMs = toMs(firstByteAt - start);

            if (httpStatus >= 200 && httpStatus < 500) {
                int slowThreshold = site.getSlowThresholdMs() == null ? 600 : site.getSlowThresholdMs();
                status = (rtt > slowThreshold) ? SiteStatus.SLOW : SiteStatus.OK;
            } else {
                status = SiteStatus.DOWN;
                err = "HTTP " + httpStatus;
            }

            // Metrics autodetect (only if UP-ish)
            if (status != SiteStatus.DOWN) {
                MetricsProbeResult probe = probeMetrics(uri, site.getTimeoutMs() == null ? 5000 : site.getTimeoutMs());
                if (probe != null) {
                    metricsEndpoint = probe.endpoint;
                    metricsJson = probe.metricsJson;
                }
            }

        } catch (Exception ex) {
            long end = System.nanoTime();
            rtt = toMs(end - startAll);
            status = SiteStatus.DOWN;
            err = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "erro" : ex.getMessage());
        }

        SiteCheckEntity check = SiteCheckEntity.builder()
                .site(site)
                .checkedAt(LocalDateTime.now())
                .status(status)
                .rttMs(rtt)
                .httpStatus(httpStatus)
                .dnsMs(dnsMs)
                .tlsMs(tlsMs)
                .ttfbMs(ttfbMs)
                .bytesRead(bytesRead)
                .resolvedIp(resolvedIp)
                .finalUrl(finalUrl)
                .redirectsCount(redirectsCount)
                .sslValidTo(sslValidTo)
                .sslDaysLeft(sslDaysLeft)
                .metricsEndpoint(metricsEndpoint)
                .metricsJson(metricsJson)
                .errorMessage(err)
                .build();

        SiteCheckEntity saved = checkRepository.save(check);

        // Incidentes: abre no DOWN e fecha ao voltar para OK/SLOW
        IncidentEntity openIncident = incidentRepository.findTop1BySite_IdAndStatusOrderByOpenedAtDesc(site.getId(), IncidentStatus.OPEN)
                .orElse(null);

        if (status == SiteStatus.DOWN) {
            if (openIncident == null) {
                IncidentEntity inc = IncidentEntity.builder()
                        .site(site)
                        .status(IncidentStatus.OPEN)
                        .openedAt(LocalDateTime.now())
                        .message(err)
                        .lastCheck(saved)
                        .build();
                incidentRepository.save(inc);
                // Pushover: notifica que caiu
                try {
                    pushoverService.notifyIncidentOpened(site.getName(), site.getUrl(), err);
                } catch (Exception ignored) {}
            } else {
                openIncident.setMessage(err);
                openIncident.setLastCheck(saved);
                incidentRepository.save(openIncident);
            }
        } else {
            if (openIncident != null) {
                openIncident.setStatus(IncidentStatus.CLOSED);
                openIncident.setClosedAt(LocalDateTime.now());
                openIncident.setLastCheck(saved);
                incidentRepository.save(openIncident);
                // Pushover: notifica que voltou
                try {
                    pushoverService.notifyIncidentClosed(site.getName(), site.getUrl());
                } catch (Exception ignored) {}
            }
            // Pushover: alerta SSL expirando (<=14 dias)
            if (sslDaysLeft != null && sslDaysLeft <= 14) {
                try {
                    pushoverService.notifySslExpiring(site.getName(), site.getUrl(), sslDaysLeft);
                } catch (Exception ignored) {}
            }
        }
    }

    private MonitoredSiteResponse toResponse(MonitoredSiteEntity site, LocalDateTime since30d) {
        SiteCheckEntity latest = checkRepository.findTop1BySite_IdOrderByCheckedAtDesc(site.getId()).orElse(null);

        SiteStatus status = latest == null ? SiteStatus.DOWN : latest.getStatus();
        LocalDateTime lastCheckAt = latest == null ? null : latest.getCheckedAt();
        Integer rtt = latest == null ? null : latest.getRttMs();

        long total = checkRepository.countSince(site.getId(), since30d);
        long up = checkRepository.countUpSince(site.getId(), since30d, SiteStatus.DOWN);

        double uptime = (total <= 0) ? 100.0 : (up * 100.0) / total;

        return new MonitoredSiteResponse(
                site.getId(),
                site.getCode(),
                site.getName(),
                site.getUrl(),
                site.getEnabled(),
                status,
                uptime,
                lastCheckAt,
                rtt,
                site.getSlowThresholdMs(),
                site.getTimeoutMs()
        );
    }

    private SiteCheckResponse toCheckResponse(SiteCheckEntity c) {
        return new SiteCheckResponse(
                c.getId(),
                c.getCheckedAt(),
                c.getStatus(),
                c.getRttMs(),
                c.getHttpStatus(),
                c.getDnsMs(),
                c.getTlsMs(),
                c.getTtfbMs(),
                c.getBytesRead(),
                c.getResolvedIp(),
                c.getFinalUrl(),
                c.getRedirectsCount(),
                c.getSslValidTo(),
                c.getSslDaysLeft(),
                c.getMetricsEndpoint(),
                c.getMetricsJson(),
                c.getErrorMessage()
        );
    }

    private String guessNameFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return "Site";
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ignored) {
            return "Site";
        }
    }

    private Integer toMs(long nanos) {
        return (int) Math.max(0, Math.round(nanos / 1_000_000.0));
    }

    private String resolveIp(String host) {
        try {
            if (host == null) return null;
            InetAddress addr = InetAddress.getByName(host);
            return addr.getHostAddress();
        } catch (Exception ex) {
            return null;
        }
    }

    private record SSLInfo(Integer tlsMs, LocalDateTime validTo, Integer daysLeft) {}
    private SSLInfo fetchSslInfo(URI uri) {
        try {
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 443;

            long start = System.nanoTime();
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(3000);
                socket.startHandshake();
                long end = System.nanoTime();

                SSLSession session = socket.getSession();
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                Date notAfter = cert.getNotAfter();

                LocalDateTime validTo = LocalDateTime.ofInstant(notAfter.toInstant(), ZoneId.systemDefault());
                long daysLeft = Duration.between(LocalDateTime.now(), validTo).toDays();

                return new SSLInfo(toMs(end - start), validTo, (int) daysLeft);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private record MetricsProbeResult(String endpoint, String metricsJson) {}

    /**
     * Option 3 (prometheus/actuator auto-detect):
     * tenta endpoints comuns e, quando encontra saída Prometheus, extrai um "resumo" com métricas úteis.
     */
    private MetricsProbeResult probeMetrics(URI siteUri, int timeoutMs) {
        try {
            String base = siteUri.getScheme() + "://" + siteUri.getHost() + (siteUri.getPort() > 0 ? ":" + siteUri.getPort() : "");
            List<String> candidates = List.of(
                    "/actuator/prometheus",
                    "/prometheus",
                    "/metrics",
                    "/metrics/prometheus",
                    "/actuator/metrics"
            );

            for (String path : candidates) {
                String url = base + path;

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(Math.min(3000, timeoutMs)))
                        .GET()
                        .header("User-Agent", "Decode-Monitor/1.0")
                        .header("Accept", "text/plain,application/openmetrics-text,application/json,*/*")
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) continue;

                String body = resp.body() == null ? "" : resp.body();
                if (looksLikePrometheus(body)) {
                    Map<String, Object> summary = summarizePrometheus(body);
                    return new MetricsProbeResult(url, toJson(summary));
                }

                // Actuator /metrics (JSON) - sem detalhar nomes (muitos exigem query por métrica),
                // mas ainda dá para indicar que existe e está exposto.
                if (path.equals("/actuator/metrics") && body.trim().startsWith("{")) {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("type", "actuator_metrics");
                    summary.put("endpoint", url);
                    summary.put("note", "Actuator metrics endpoint detected. Specific metric values may require querying /actuator/metrics/{name}.");
                    return new MetricsProbeResult(url, toJson(summary));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean looksLikePrometheus(String body) {
        String b = body == null ? "" : body;
        return b.contains("# HELP") || b.contains("# TYPE") || b.contains("process_cpu") || b.contains("jvm_memory");
    }

    private Map<String, Object> summarizePrometheus(String body) {
        Map<String, Double> values = parsePrometheus(body);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "prometheus");
        out.put("keys", values.size());

        // Selected best-effort metrics (vary a lot by stack)
        putIfPresent(out, values, "up");
        putIfPresent(out, values, "process_uptime_seconds");
        putIfPresent(out, values, "process_start_time_seconds");
        putIfPresent(out, values, "process_cpu_usage");
        putIfPresent(out, values, "system_cpu_usage");
        putIfPresent(out, values, "jvm_memory_used_bytes");
        putIfPresent(out, values, "jvm_memory_max_bytes");
        putIfPresent(out, values, "process_resident_memory_bytes");
        putIfPresent(out, values, "nodejs_heap_size_used_bytes");
        putIfPresent(out, values, "nodejs_heap_size_total_bytes");
        putIfPresent(out, values, "go_memstats_alloc_bytes");
        putIfPresent(out, values, "go_memstats_sys_bytes");

        // generic "http" signals (may be absent)
        putIfPresent(out, values, "http_server_requests_seconds_count");
        putIfPresent(out, values, "http_server_requests_seconds_sum");

        return out;
    }

    private void putIfPresent(Map<String, Object> out, Map<String, Double> values, String key) {
        if (values.containsKey(key)) out.put(key, values.get(key));
    }

    /**
     * Prometheus text parser (best-effort): keeps only unlabelled samples (no {...})
     * to avoid exploding cardinality.
     */
    private Map<String, Double> parsePrometheus(String body) {
        Map<String, Double> map = new LinkedHashMap<>();
        String[] lines = body.split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            // ignore labelled metrics for safety
            if (line.contains("{")) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 2) continue;

            String name = parts[0].trim();
            String valStr = parts[1].trim();
            try {
                double v = Double.parseDouble(valStr);
                map.put(name, v);
            } catch (Exception ignored) {}
        }
        return map;
    }

    private String toJson(Map<String, Object> map) {
        // minimal JSON builder without extra deps
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else {
                sb.append("\"").append(escapeJson(v.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
