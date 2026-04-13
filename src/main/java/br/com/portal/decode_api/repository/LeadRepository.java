package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.LeadEntity;
import br.com.portal.decode_api.enums.LeadStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LeadRepository extends JpaRepository<LeadEntity, UUID> {

    @Query("""
            select l
            from LeadEntity l
            left join l.ownerUser u
            where (:q is null or :q = ''
                or lower(l.code) like lower(concat(concat('%', :q), '%'))
                or lower(l.name) like lower(concat(concat('%', :q), '%'))
                or (u is not null and lower(u.name) like lower(concat(concat('%', :q), '%')))
            )
            order by l.updatedAt desc
            """)
    Page<LeadEntity> search(@Param("q") String q, Pageable pageable);

    long countByStage(LeadStage stage);

    @Query("""
            select l
            from LeadEntity l
            where l.affiliate.id = :affiliateId
              and (:q is null or :q = ''
                or lower(l.code) like lower(concat(concat('%', :q), '%'))
                or lower(l.name) like lower(concat(concat('%', :q), '%'))
              )
            order by l.updatedAt desc
            """)
    Page<LeadEntity> searchByAffiliate(@Param("affiliateId") UUID affiliateId,
                                       @Param("q") String q,
                                       Pageable pageable);

    long countByAffiliateId(UUID affiliateId);

    long countByAffiliateIdAndStage(UUID affiliateId, LeadStage stage);
}
