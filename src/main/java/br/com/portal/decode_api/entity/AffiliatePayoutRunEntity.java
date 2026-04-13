package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.AffiliatePayoutRunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "affiliate_payout_runs",
       uniqueConstraints = @UniqueConstraint(name = "uk_payout_month", columnNames = "reference_month"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliatePayoutRunEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    /** Primeiro dia do mes que esta sendo pago. */
    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "total_affiliates", nullable = false)
    private Integer totalAffiliates;

    @Column(name = "total_commissions", nullable = false)
    private Integer totalCommissions;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliatePayoutRunStatus status;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = AffiliatePayoutRunStatus.DRAFT;
        if (totalAffiliates == null) totalAffiliates = 0;
        if (totalCommissions == null) totalCommissions = 0;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (generatedAt == null) generatedAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
