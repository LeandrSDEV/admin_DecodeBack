package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.dtos.affiliate.AffiliateMarkPaidRequest;
import br.com.portal.decode_api.dtos.affiliate.AffiliatePayoutRunResponse;
import br.com.portal.decode_api.entity.AffiliateCommissionEntity;
import br.com.portal.decode_api.entity.AffiliatePayoutRunEntity;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.AffiliateCommissionStatus;
import br.com.portal.decode_api.enums.AffiliatePayoutRunStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.AffiliateCommissionRepository;
import br.com.portal.decode_api.repository.AffiliatePayoutRunRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AffiliatePayoutRunService {

    private static final Logger log = LoggerFactory.getLogger(AffiliatePayoutRunService.class);

    private final AffiliatePayoutRunRepository runRepository;
    private final AffiliateCommissionRepository commissionRepository;

    /**
     * Cria um novo payout run DRAFT para o mes informado, agrupando todas as comissoes
     * APROVADAS pendentes de pagamento cujo referenceMonth <= mes informado.
     */
    @Transactional
    public AffiliatePayoutRunResponse createDraft(LocalDate referenceMonth) {
        LocalDate normalized = referenceMonth.withDayOfMonth(1);

        if (runRepository.findByReferenceMonth(normalized).isPresent()) {
            throw new IllegalStateException("Ja existe um payout run para o mes " + normalized);
        }

        List<AffiliateCommissionEntity> payable = commissionRepository.findPayableUpTo(
                AffiliateCommissionStatus.APPROVED, normalized);

        if (payable.isEmpty()) {
            throw new IllegalStateException("Nenhuma comissao aprovada pendente para o mes " + normalized);
        }

        Set<UUID> affiliates = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AffiliateCommissionEntity c : payable) {
            affiliates.add(c.getAffiliate().getId());
            total = total.add(c.getCommissionAmount());
        }

        AffiliatePayoutRunEntity run = AffiliatePayoutRunEntity.builder()
                .referenceMonth(normalized)
                .totalAffiliates(affiliates.size())
                .totalCommissions(payable.size())
                .totalAmount(total)
                .status(AffiliatePayoutRunStatus.DRAFT)
                .build();
        run = runRepository.save(run);

        for (AffiliateCommissionEntity c : payable) {
            c.setPayoutRun(run);
            commissionRepository.save(c);
        }

        log.info("Payout run criado: {} (mes {}), {} comissoes, R$ {}",
                run.getId(), normalized, payable.size(), total);
        return toResponse(run);
    }

    @Transactional
    public AffiliatePayoutRunResponse markReviewed(UUID runId, UserEntity reviewer) {
        AffiliatePayoutRunEntity run = requireRun(runId);
        if (run.getStatus() != AffiliatePayoutRunStatus.DRAFT) {
            throw new IllegalStateException("Run nao esta em DRAFT: " + run.getStatus());
        }
        run.setStatus(AffiliatePayoutRunStatus.REVIEWED);
        run.setReviewedAt(LocalDateTime.now());
        run.setReviewedBy(reviewer);
        return toResponse(runRepository.save(run));
    }

    @Transactional
    public AffiliatePayoutRunResponse markExecuting(UUID runId) {
        AffiliatePayoutRunEntity run = requireRun(runId);
        if (run.getStatus() != AffiliatePayoutRunStatus.REVIEWED) {
            throw new IllegalStateException("Run nao esta em REVIEWED: " + run.getStatus());
        }
        run.setStatus(AffiliatePayoutRunStatus.EXECUTING);
        return toResponse(runRepository.save(run));
    }

    /**
     * Marca uma comissao individual como paga. Quando todas as comissoes do run estao PAID,
     * o run automaticamente vira COMPLETED.
     */
    @Transactional
    public void markCommissionPaid(UUID commissionId, AffiliateMarkPaidRequest req) {
        AffiliateCommissionEntity c = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new EntityNotFoundException("Commission", commissionId));

        if (c.getStatus() != AffiliateCommissionStatus.APPROVED) {
            throw new IllegalStateException("Comissao nao esta em APPROVED: " + c.getStatus());
        }

        c.setStatus(AffiliateCommissionStatus.PAID);
        c.setPaidAt(LocalDateTime.now());
        c.setPaidReference(req.paidReference());
        if (req.notes() != null) c.setNotes(req.notes());
        commissionRepository.save(c);

        // Checa se o run terminou
        if (c.getPayoutRun() != null) {
            UUID runId = c.getPayoutRun().getId();
            List<AffiliateCommissionEntity> all = commissionRepository.findByPayoutRunId(runId);
            boolean allPaid = all.stream().allMatch(x -> x.getStatus() == AffiliateCommissionStatus.PAID
                    || x.getStatus() == AffiliateCommissionStatus.REVERSED);
            if (allPaid) {
                AffiliatePayoutRunEntity run = c.getPayoutRun();
                run.setStatus(AffiliatePayoutRunStatus.COMPLETED);
                run.setCompletedAt(LocalDateTime.now());
                runRepository.save(run);
                log.info("Payout run {} concluido", runId);
            }
        }
    }

    @Transactional
    public AffiliatePayoutRunResponse cancel(UUID runId, String reason) {
        AffiliatePayoutRunEntity run = requireRun(runId);
        if (run.getStatus() == AffiliatePayoutRunStatus.COMPLETED) {
            throw new IllegalStateException("Nao eh possivel cancelar run ja concluido.");
        }
        // Desvincula as comissoes do run
        List<AffiliateCommissionEntity> inRun = commissionRepository.findByPayoutRunId(runId);
        for (AffiliateCommissionEntity c : inRun) {
            if (c.getStatus() == AffiliateCommissionStatus.APPROVED) {
                c.setPayoutRun(null);
                commissionRepository.save(c);
            }
        }
        run.setStatus(AffiliatePayoutRunStatus.CANCELLED);
        run.setNotes((run.getNotes() == null ? "" : run.getNotes() + "\n") + "[CANCELADO] " + reason);
        return toResponse(runRepository.save(run));
    }

    @Transactional(readOnly = true)
    public Page<AffiliatePayoutRunResponse> list(Pageable pageable) {
        return runRepository.findAllByOrderByReferenceMonthDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AffiliatePayoutRunResponse get(UUID runId) {
        return toResponse(requireRun(runId));
    }

    private AffiliatePayoutRunEntity requireRun(UUID id) {
        return runRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("PayoutRun", id));
    }

    public AffiliatePayoutRunResponse toResponse(AffiliatePayoutRunEntity r) {
        return new AffiliatePayoutRunResponse(
                r.getId(),
                r.getReferenceMonth(),
                r.getTotalAffiliates(),
                r.getTotalCommissions(),
                r.getTotalAmount(),
                r.getStatus(),
                r.getGeneratedAt(),
                r.getReviewedAt(),
                r.getCompletedAt(),
                r.getNotes()
        );
    }
}
