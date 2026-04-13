package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
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
@Table(name = "affiliate_commissions",
       uniqueConstraints = @UniqueConstraint(name = "uk_comm_unique_period",
               columnNames = {"affiliate_id", "decode_id", "reference_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateCommissionEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affiliate_id", nullable = false)
    private AffiliateEntity affiliate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decode_id", nullable = false)
    private DecodeEntity decode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription;

    /** Sempre o primeiro dia do mes de referencia (ex: 2026-04-01). */
    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "plan_name", nullable = false, length = 120)
    private String planName;

    @Column(name = "plan_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal planPrice;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliateCommissionStatus status;

    /** Data a partir da qual a comissao pode ser aprovada (carencia do 2o mes). */
    @Column(name = "carencia_until", nullable = false)
    private LocalDate carenciaUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_run_id")
    private AffiliatePayoutRunEntity payoutRun;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_reference", length = 180)
    private String paidReference;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reverse_reason", columnDefinition = "text")
    private String reverseReason;

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
        if (status == null) status = AffiliateCommissionStatus.PENDING;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
