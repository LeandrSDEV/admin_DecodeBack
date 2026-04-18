package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.AffiliateDecodeSubmissionStatus;
import br.com.portal.decode_api.enums.SubscriptionModule;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Proposta de novo estabelecimento submetida por um afiliado.
 * Fluxo: afiliado preenche dados no portal (PENDING) -> admin aprova/rejeita.
 * Na aprovacao, um {@link DecodeEntity} e uma {@link SubscriptionEntity} sao
 * criados automaticamente e vinculados ao afiliado como referral CONVERTED.
 */
@Entity
@Table(name = "affiliate_decode_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateDecodeSubmissionEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affiliate_id", nullable = false)
    private AffiliateEntity affiliate;

    @Column(name = "establishment_name", nullable = false, length = 180)
    private String establishmentName;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 4)
    private String state;

    @Column(length = 20)
    private String cnpj;

    @Column(name = "contact_name", nullable = false, length = 180)
    private String contactName;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "estimated_users_count")
    private Integer estimatedUsersCount;

    @Column(name = "estimated_monthly_revenue", precision = 12, scale = 2)
    private BigDecimal estimatedMonthlyRevenue;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_module", nullable = false, length = 20)
    private SubscriptionModule planModule;

    @Column(name = "plan_name", nullable = false, length = 120)
    private String planName;

    @Column(name = "plan_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal planPrice;

    @Column(name = "plan_discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal planDiscountPct;

    @Column(name = "plan_duration_days", nullable = false)
    private Integer planDurationDays;

    @Column(name = "plan_features", columnDefinition = "text")
    private String planFeatures;

    @Column(columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliateDecodeSubmissionStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decode_id")
    private DecodeEntity decode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = AffiliateDecodeSubmissionStatus.PENDING;
        if (submittedAt == null) submittedAt = LocalDateTime.now();
        if (planDiscountPct == null) planDiscountPct = BigDecimal.ZERO;
        if (planDurationDays == null) planDurationDays = 30;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
