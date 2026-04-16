package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.SubscriptionModule;
import br.com.portal.decode_api.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decode_id")
    private DecodeEntity decode;

    @Column(name = "establishment_name", length = 160)
    private String establishmentName;

    @Column(name = "client_name", length = 160)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionModule module;

    @Column(name = "plan_name", nullable = false, length = 120)
    private String planName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(columnDefinition = "text")
    private String features;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "text")
    private String cancelReason;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = SubscriptionStatus.ACTIVE;
        if (discountPct == null) discountPct = BigDecimal.ZERO;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && !isExpired();
    }
}
