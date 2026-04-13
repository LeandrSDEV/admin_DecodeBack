package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.AffiliatePayoutRunEntity;
import br.com.portal.decode_api.enums.AffiliatePayoutRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AffiliatePayoutRunRepository extends JpaRepository<AffiliatePayoutRunEntity, UUID> {

    Optional<AffiliatePayoutRunEntity> findByReferenceMonth(LocalDate referenceMonth);

    Page<AffiliatePayoutRunEntity> findAllByOrderByReferenceMonthDesc(Pageable pageable);

    boolean existsByStatusAndReferenceMonth(AffiliatePayoutRunStatus status, LocalDate referenceMonth);
}
