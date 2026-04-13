package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.DecodeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "decodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecodeEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 120)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecodeStatus status;

    @Column(name = "users_count", nullable = false)
    private Integer usersCount;

    @Column(name = "monthly_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRevenue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliate_id")
    private AffiliateEntity affiliate;

    @Column(name = "affiliate_attached_at")
    private LocalDateTime affiliateAttachedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (code == null || code.isBlank()) code = "D-" + id.toString().substring(0, 6).toUpperCase();
        if (status == null) status = DecodeStatus.ACTIVE;
        if (usersCount == null) usersCount = 0;
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
