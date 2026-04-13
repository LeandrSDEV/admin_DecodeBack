package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.AffiliateReferralStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "affiliate_referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateReferralEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affiliate_id", nullable = false)
    private AffiliateEntity affiliate;

    @Column(name = "first_touch_at", nullable = false)
    private LocalDateTime firstTouchAt;

    @Column(name = "source_ip", length = 60)
    private String sourceIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "landing_url", length = 500)
    private String landingUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliateReferralStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private LeadEntity lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decode_id")
    private DecodeEntity decode;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "churned_at")
    private LocalDateTime churnedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = AffiliateReferralStatus.CLICKED;
        if (firstTouchAt == null) firstTouchAt = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
