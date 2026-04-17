package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.DecodeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DecodeRepository extends JpaRepository<DecodeEntity, UUID> {

    @Query(value = """
            select d
            from DecodeEntity d
            left join fetch d.affiliate
            where (:q is null or :q = ''
                or lower(d.code) like lower(concat(concat('%', :q), '%'))
                or lower(d.name) like lower(concat(concat('%', :q), '%'))
                or lower(d.city) like lower(concat(concat('%', :q), '%'))
            )
            order by d.updatedAt desc
            """,
            countQuery = """
            select count(d)
            from DecodeEntity d
            where (:q is null or :q = ''
                or lower(d.code) like lower(concat(concat('%', :q), '%'))
                or lower(d.name) like lower(concat(concat('%', :q), '%'))
                or lower(d.city) like lower(concat(concat('%', :q), '%'))
            )
            """)
    Page<DecodeEntity> search(@Param("q") String q, Pageable pageable);

    @Query("select d from DecodeEntity d where d.affiliate.id = :affiliateId")
    List<DecodeEntity> findByAffiliateId(@Param("affiliateId") UUID affiliateId);

    @Query("select d from DecodeEntity d where d.affiliate is not null")
    List<DecodeEntity> findAllWithAffiliate();

    @Query("""
            select count(d) from DecodeEntity d
            where d.affiliate.id = :affiliateId
              and d.status = br.com.portal.decode_api.enums.DecodeStatus.ACTIVE
            """)
    long countActiveByAffiliateId(@Param("affiliateId") UUID affiliateId);

    @Query("""
            select coalesce(sum(d.monthlyRevenue), 0) from DecodeEntity d
            where d.affiliate.id = :affiliateId
            """)
    java.math.BigDecimal sumMonthlyRevenueByAffiliateId(@Param("affiliateId") UUID affiliateId);

    @Query("""
            select count(d) from DecodeEntity d
            where d.affiliate.id = :affiliateId
            """)
    long countByAffiliateId(@Param("affiliateId") UUID affiliateId);

    @Query("""
            select count(d) from DecodeEntity d
            where d.affiliate.id = :affiliateId
              and coalesce(d.affiliateAttachedAt, d.createdAt) >= :start
              and coalesce(d.affiliateAttachedAt, d.createdAt) < :end
            """)
    long countByAffiliateIdAndAttachedBetween(@Param("affiliateId") UUID affiliateId,
                                               @Param("start") java.time.LocalDateTime start,
                                               @Param("end") java.time.LocalDateTime end);

    @Query("""
            select cast(coalesce(d.affiliateAttachedAt, d.createdAt) as date) as day,
                   count(d),
                   coalesce(sum(d.monthlyRevenue), 0)
            from DecodeEntity d
            where d.affiliate.id = :affiliateId
              and coalesce(d.affiliateAttachedAt, d.createdAt) >= :start
            group by cast(coalesce(d.affiliateAttachedAt, d.createdAt) as date)
            order by day
            """)
    List<Object[]> dailyProductionByAffiliateSince(@Param("affiliateId") UUID affiliateId,
                                                    @Param("start") java.time.LocalDateTime start);
}
