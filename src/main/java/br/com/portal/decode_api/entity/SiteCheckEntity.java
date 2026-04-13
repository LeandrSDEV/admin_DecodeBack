package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.SiteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "site_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteCheckEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private MonitoredSiteEntity site;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteStatus status;

    @Column(name = "rtt_ms", nullable = false)
    private Integer rttMs;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error_message")
    private String errorMessage;

    // Extended measurements
    @Column(name = "dns_ms")
    private Integer dnsMs;

    @Column(name = "tls_ms")
    private Integer tlsMs;

    @Column(name = "ttfb_ms")
    private Integer ttfbMs;

    @Column(name = "bytes_read")
    private Long bytesRead;

    @Column(name = "resolved_ip", length = 64)
    private String resolvedIp;

    @Column(name = "final_url", columnDefinition = "text")
    private String finalUrl;

    @Column(name = "redirects_count")
    private Integer redirectsCount;

    @Column(name = "ssl_valid_to")
    private LocalDateTime sslValidTo;

    @Column(name = "ssl_days_left")
    private Integer sslDaysLeft;

    @Column(name = "metrics_endpoint", columnDefinition = "text")
    private String metricsEndpoint;

    @Column(name = "metrics_json", columnDefinition = "text")
    private String metricsJson;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (checkedAt == null) checkedAt = LocalDateTime.now();
        if (status == null) status = SiteStatus.DOWN;
        if (rttMs == null) rttMs = 0;
    }
}
