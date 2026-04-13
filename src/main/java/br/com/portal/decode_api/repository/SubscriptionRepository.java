package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.SubscriptionEntity;
import br.com.portal.decode_api.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Page<SubscriptionEntity> findByDecodeIdOrderByStartedAtDesc(UUID decodeId, Pageable pageable);

    Optional<SubscriptionEntity> findFirstByDecodeIdAndStatusOrderByStartedAtDesc(UUID decodeId, SubscriptionStatus status);

    @Query("""
            select s from SubscriptionEntity s
            left join s.decode d
            where (:q is null or :q = ''
                or lower(coalesce(d.name, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(s.establishmentName, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(s.clientName, '')) like lower(concat('%', :q, '%'))
                or lower(s.planName) like lower(concat('%', :q, '%'))
            )
            order by s.startedAt desc
            """)
    Page<SubscriptionEntity> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select coalesce(sum(s.price * (1 - coalesce(s.discountPct, 0) / 100.0)), 0)
            from SubscriptionEntity s
            where s.decode.affiliate.id = :affiliateId
              and s.status = br.com.portal.decode_api.enums.SubscriptionStatus.ACTIVE
            """)
    java.math.BigDecimal sumActivePlanAmountByAffiliateId(@Param("affiliateId") UUID affiliateId);
}
