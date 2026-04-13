package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.InteractionEntity;
import br.com.portal.decode_api.enums.InteractionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InteractionRepository extends JpaRepository<InteractionEntity, UUID> {

    @Query("""
            select i
            from InteractionEntity i
            left join i.ownerUser u
            where (:q is null or :q = ''
                or lower(i.code) like lower(concat(concat('%', :q), '%'))
                or lower(i.contactName) like lower(concat(concat('%', :q), '%'))
                or lower(i.city) like lower(concat(concat('%', :q), '%'))
                or (u is not null and lower(u.name) like lower(concat(concat('%', :q), '%')))
            )
            order by i.updatedAt desc
            """)
    Page<InteractionEntity> search(@Param("q") String q, Pageable pageable);

    long countByStatus(InteractionStatus status);

    @Query("""
            select i
            from InteractionEntity i
            where i.lead.affiliate.id = :affiliateId
              and (:q is null or :q = ''
                or lower(i.code) like lower(concat(concat('%', :q), '%'))
                or lower(i.contactName) like lower(concat(concat('%', :q), '%'))
                or lower(i.city) like lower(concat(concat('%', :q), '%'))
              )
            order by i.updatedAt desc
            """)
    Page<InteractionEntity> searchByAffiliate(@Param("affiliateId") UUID affiliateId,
                                              @Param("q") String q,
                                              Pageable pageable);

    @Query("select count(i) from InteractionEntity i where i.lead.affiliate.id = :affiliateId")
    long countByAffiliateId(@Param("affiliateId") UUID affiliateId);

    @Query("select count(i) from InteractionEntity i where i.lead.affiliate.id = :affiliateId and i.status = :status")
    long countByAffiliateIdAndStatus(@Param("affiliateId") UUID affiliateId,
                                     @Param("status") InteractionStatus status);
}
