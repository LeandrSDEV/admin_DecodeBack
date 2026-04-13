package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.AffiliateReferralEntity;
import br.com.portal.decode_api.enums.AffiliateReferralStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffiliateReferralRepository extends JpaRepository<AffiliateReferralEntity, UUID> {

    Optional<AffiliateReferralEntity> findByLeadId(UUID leadId);

    Optional<AffiliateReferralEntity> findByDecodeId(UUID decodeId);

    Page<AffiliateReferralEntity> findByAffiliateIdOrderByCreatedAtDesc(UUID affiliateId, Pageable pageable);

    List<AffiliateReferralEntity> findByAffiliateIdAndStatus(UUID affiliateId, AffiliateReferralStatus status);

    List<AffiliateReferralEntity> findByAffiliateId(UUID affiliateId);

    @Query("""
            select count(r) from AffiliateReferralEntity r
            where r.affiliate.id = :affiliateId
              and r.status = 'CONVERTED'
              and r.convertedAt >= :from
              and r.convertedAt < :to
            """)
    long countNewConversionsInPeriod(@Param("affiliateId") UUID affiliateId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    long countByAffiliateIdAndStatus(UUID affiliateId, AffiliateReferralStatus status);
}
