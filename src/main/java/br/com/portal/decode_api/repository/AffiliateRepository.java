package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.enums.AffiliateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AffiliateRepository extends JpaRepository<AffiliateEntity, UUID> {

    Optional<AffiliateEntity> findByRefCode(String refCode);

    Optional<AffiliateEntity> findByEmailIgnoreCase(String email);

    boolean existsByRefCode(String refCode);

    boolean existsByEmailIgnoreCase(String email);

    Page<AffiliateEntity> findByStatus(AffiliateStatus status, Pageable pageable);

    @Query("""
            select a from AffiliateEntity a
            where (:q is null or :q = ''
                or lower(a.name) like lower(concat('%', :q, '%'))
                or lower(a.email) like lower(concat('%', :q, '%'))
                or lower(a.refCode) like lower(concat('%', :q, '%'))
                or a.whatsapp like concat('%', :q, '%'))
              and (:status is null or a.status = :status)
            order by a.createdAt desc
            """)
    Page<AffiliateEntity> search(@Param("q") String q,
                                  @Param("status") AffiliateStatus status,
                                  Pageable pageable);

    long countByStatus(AffiliateStatus status);
}
