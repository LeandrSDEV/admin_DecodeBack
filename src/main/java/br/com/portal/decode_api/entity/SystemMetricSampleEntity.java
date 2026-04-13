package br.com.portal.decode_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_metric_samples")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricSampleEntity {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR) // <- força salvar como texto (36)
    @Column(name = "id", columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "sampled_at", nullable = false)
    private LocalDateTime sampledAt;

    @Column(name = "cpu_load", nullable = false)
    private Double cpuLoad;

    @Column(name = "mem_used_bytes", nullable = false)
    private Long memUsedBytes;

    @Column(name = "mem_total_bytes", nullable = false)
    private Long memTotalBytes;

    @Column(name = "disk_used_bytes", nullable = false)
    private Long diskUsedBytes;

    @Column(name = "disk_total_bytes", nullable = false)
    private Long diskTotalBytes;

    @Column(name = "uptime_seconds", nullable = false)
    private Long uptimeSeconds;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (sampledAt == null) sampledAt = LocalDateTime.now();
    }
}