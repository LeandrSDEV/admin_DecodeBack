package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.SubscriptionEntity;
import br.com.portal.decode_api.enums.SubscriptionModule;
import br.com.portal.decode_api.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Page<SubscriptionEntity> findByDecodeIdOrderByStartedAtDesc(UUID decodeId, Pageable pageable);

    Optional<SubscriptionEntity> findFirstByDecodeIdAndStatusOrderByStartedAtDesc(UUID decodeId, SubscriptionStatus status);

    /** Busca assinatura ativa de um decode para um módulo específico. */
    Optional<SubscriptionEntity> findFirstByDecodeIdAndModuleAndStatusOrderByStartedAtDesc(
            UUID decodeId, SubscriptionModule module, SubscriptionStatus status);

    /** Todas as assinaturas ativas de um decode (pode haver uma MESA e uma DELIVERY). */
    List<SubscriptionEntity> findAllByDecodeIdAndStatus(UUID decodeId, SubscriptionStatus status);

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
