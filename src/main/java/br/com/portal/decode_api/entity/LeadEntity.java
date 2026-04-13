package br.com.portal.decode_api.entity;

import br.com.portal.decode_api.enums.LeadSource;
import br.com.portal.decode_api.enums.LeadStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String status;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "last_contact_at")
    private LocalDateTime lastContactAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private UserEntity ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliate_id")
    private AffiliateEntity affiliate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (code == null || code.isBlank()) code = "L-" + id.toString().substring(0, 6).toUpperCase();
        if (source == null) source = LeadSource.WHATSAPP;
        if (stage == null) stage = LeadStage.WAITING;
        if (score == null) score = 0;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
