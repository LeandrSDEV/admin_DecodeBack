package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.AffiliateDecodeSubmissionEntity;
import br.com.portal.decode_api.enums.AffiliateDecodeSubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AffiliateDecodeSubmissionRepository
        extends JpaRepository<AffiliateDecodeSubmissionEntity, UUID> {

    Page<AffiliateDecodeSubmissionEntity> findByAffiliateIdOrderBySubmittedAtDesc(UUID affiliateId, Pageable pageable);

    long countByStatus(AffiliateDecodeSubmissionStatus status);

    @Query("""
            select s from AffiliateDecodeSubmissionEntity s
            where (:status is null or s.status = :status)
              and (:q is null or :q = ''
                   or lower(s.establishmentName) like lower(concat('%', :q, '%'))
                   or lower(s.contactName) like lower(concat('%', :q, '%'))
                   or lower(s.contactPhone) like lower(concat('%', :q, '%'))
                   or lower(s.city) like lower(concat('%', :q, '%'))
                   or lower(s.affiliate.name) like lower(concat('%', :q, '%'))
                   or lower(s.affiliate.refCode) like lower(concat('%', :q, '%')))
            order by s.submittedAt desc
            """)
    Page<AffiliateDecodeSubmissionEntity> search(@Param("q") String q,
                                                  @Param("status") AffiliateDecodeSubmissionStatus status,
                                                  Pageable pageable);
}
