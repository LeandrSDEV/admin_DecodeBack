package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.enums.AffiliateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Top N afiliados ATIVOS ordenados pelo valor total de comissões somadas
     * (PENDING + APPROVED + PAID). Usado pelo pedestal do dashboard.
     *
     * Retorna projeção: [affiliateId UUID, name String, refCode String,
     *                    activeClients long, totalEarned BigDecimal]
     */
    @Query("""
            select a.id, a.name, a.refCode,
                   coalesce(count(distinct case when d.status = br.com.portal.decode_api.enums.DecodeStatus.ACTIVE then d.id else null end), 0) as activeClients,
                   coalesce(sum(case when c.status in (br.com.portal.decode_api.enums.AffiliateCommissionStatus.PENDING,
                                                        br.com.portal.decode_api.enums.AffiliateCommissionStatus.APPROVED,
                                                        br.com.portal.decode_api.enums.AffiliateCommissionStatus.PAID)
                                    then c.commissionAmount else 0 end), 0) as totalEarned
            from AffiliateEntity a
            left join DecodeEntity d on d.affiliate.id = a.id
            left join AffiliateCommissionEntity c on c.affiliate.id = a.id
            where a.status = br.com.portal.decode_api.enums.AffiliateStatus.ACTIVE
            group by a.id, a.name, a.refCode
            order by totalEarned desc, activeClients desc
            """)
    List<Object[]> findTopBySalesAndConversions(Pageable pageable);
}

