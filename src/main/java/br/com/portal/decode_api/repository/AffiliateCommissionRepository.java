package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.AffiliateCommissionEntity;
import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffiliateCommissionRepository extends JpaRepository<AffiliateCommissionEntity, UUID> {

    Optional<AffiliateCommissionEntity> findByAffiliateIdAndDecodeIdAndReferenceMonth(
            UUID affiliateId, UUID decodeId, LocalDate referenceMonth);

    Page<AffiliateCommissionEntity> findByAffiliateIdOrderByReferenceMonthDesc(UUID affiliateId, Pageable pageable);

    List<AffiliateCommissionEntity> findByAffiliateIdAndReferenceMonth(UUID affiliateId, LocalDate referenceMonth);

    List<AffiliateCommissionEntity> findByStatusAndCarenciaUntilLessThanEqual(
            AffiliateCommissionStatus status, LocalDate date);

    List<AffiliateCommissionEntity> findByPayoutRunId(UUID payoutRunId);

    List<AffiliateCommissionEntity> findByAffiliateId(UUID affiliateId);

    List<AffiliateCommissionEntity> findByReferenceMonthAndStatus(LocalDate referenceMonth, AffiliateCommissionStatus status);

    @Query("""
            select coalesce(sum(c.commissionAmount), 0) from AffiliateCommissionEntity c
            where c.affiliate.id = :affiliateId and c.status = :status
            """)
    BigDecimal sumAmountByAffiliateAndStatus(@Param("affiliateId") UUID affiliateId,
                                              @Param("status") AffiliateCommissionStatus status);

    @Query("""
            select coalesce(sum(c.commissionAmount), 0) from AffiliateCommissionEntity c
            where c.affiliate.id = :affiliateId
              and c.referenceMonth = :referenceMonth
            """)
    BigDecimal sumAmountByAffiliateAndMonth(@Param("affiliateId") UUID affiliateId,
                                             @Param("referenceMonth") LocalDate referenceMonth);

    @Query("""
            select c from AffiliateCommissionEntity c
            where c.status = :status
              and c.payoutRun is null
              and c.referenceMonth <= :maxMonth
            order by c.affiliate.id, c.referenceMonth
            """)
    List<AffiliateCommissionEntity> findPayableUpTo(@Param("status") AffiliateCommissionStatus status,
                                                     @Param("maxMonth") LocalDate maxMonth);
}
