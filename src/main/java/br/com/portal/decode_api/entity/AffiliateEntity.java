package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.enums.PixKeyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "affiliates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "ref_code", nullable = false, length = 40, unique = true)
    private String refCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 180, unique = true)
    private String email;

    @Column(nullable = false, length = 30)
    private String whatsapp;

    @Column(length = 14)
    private String cpf;

    @Column(length = 120)
    private String city;

    @Column(length = 4)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(name = "pix_key_type", length = 20)
    private PixKeyType pixKeyType;

    @Column(name = "pix_key", length = 180)
    private String pixKey;

    @Column(name = "bank_holder", length = 180)
    private String bankHolder;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "must_change_pw", nullable = false)
    private Boolean mustChangePw;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliateStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserEntity approvedBy;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_accepted_version", length = 20)
    private String termsAcceptedVersion;

    @Column(name = "custom_commission_rate", precision = 5, scale = 2)
    private BigDecimal customCommissionRate;

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
        if (status == null) status = AffiliateStatus.PENDING;
        if (mustChangePw == null) mustChangePw = Boolean.TRUE;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == AffiliateStatus.ACTIVE;
    }
}
