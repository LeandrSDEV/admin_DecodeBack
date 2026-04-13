package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.entity.InteractionEntity;
import br.com.portal.decode_api.entity.LeadEntity;
import br.com.portal.decode_api.enums.InteractionChannel;
import br.com.portal.decode_api.enums.InteractionStatus;
import br.com.portal.decode_api.enums.LeadSource;
import br.com.portal.decode_api.enums.LeadStage;

import java.util.LinkedHashMap;
import java.util.Map;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.repository.InteractionRepository;
import br.com.portal.decode_api.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CRUD de Leads e Interações restrito ao afiliado autenticado.
 * Toda operação valida que o recurso pertence ao afiliado, evitando que um
 * afiliado consiga ler/alterar dados de outro.
 */
@Service
@RequiredArgsConstructor
public class AffiliateCrmService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateCrmService.class);

    private final LeadRepository leadRepository;
    private final InteractionRepository interactionRepository;
    private final AffiliateRepository affiliateRepository;

    // -----------------------------------------------------------------
    // LEADS
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<LeadResponse> listLeads(UUID affiliateId, String q, Pageable pageable) {
        return leadRepository.searchByAffiliate(affiliateId, q, pageable).map(this::toLeadResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(UUID affiliateId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalLeads", leadRepository.countByAffiliateId(affiliateId));
        out.put("leadsWaiting", leadRepository.countByAffiliateIdAndStage(affiliateId, LeadStage.WAITING));
        out.put("leadsMeeting", leadRepository.countByAffiliateIdAndStage(affiliateId, LeadStage.MEETING));
        out.put("leadsProposal", leadRepository.countByAffiliateIdAndStage(affiliateId, LeadStage.PROPOSAL));
        out.put("totalInteractions", interactionRepository.countByAffiliateId(affiliateId));
        out.put("interactionsWaiting", interactionRepository.countByAffiliateIdAndStatus(affiliateId, InteractionStatus.WAITING));
        out.put("interactionsAnswered", interactionRepository.countByAffiliateIdAndStatus(affiliateId, InteractionStatus.ANSWERED));
        out.put("interactionsNoResponse", interactionRepository.countByAffiliateIdAndStatus(affiliateId, InteractionStatus.NO_RESPONSE));
        return out;
    }

    @Transactional
    public LeadResponse createLead(UUID affiliateId, LeadCreateRequest req) {
        AffiliateEntity affiliate = affiliateRepository.findById(affiliateId)
                .orElseThrow(() -> new EntityNotFoundException("Afiliado", affiliateId));

        LeadEntity entity = LeadEntity.builder()
                .name(req.name().trim())
                .phone(req.phone())
                .email(req.email())
                .status(req.status())
                .score(req.score() == null ? 0 : req.score())
                .source(req.source() == null ? LeadSource.WHATSAPP : req.source())
                .stage(req.stage() == null ? LeadStage.WAITING : req.stage())
                .affiliate(affiliate)
                .build();

        LeadEntity saved = leadRepository.save(entity);
        log.info("Lead criado pelo afiliado {}: id={}, code={}", affiliateId, saved.getId(), saved.getCode());
        return toLeadResponse(saved);
    }

    @Transactional
    public LeadResponse updateLead(UUID affiliateId, UUID id, LeadUpdateRequest req) {
        LeadEntity entity = requireOwnedLead(affiliateId, id);

        if (req.name() != null && !req.name().isBlank()) entity.setName(req.name().trim());
        if (req.phone() != null) entity.setPhone(req.phone());
        if (req.email() != null) entity.setEmail(req.email());
        if (req.status() != null) entity.setStatus(req.status());
        if (req.score() != null) entity.setScore(req.score());
        if (req.source() != null) entity.setSource(req.source());
        if (req.stage() != null) entity.setStage(req.stage());

        return toLeadResponse(leadRepository.save(entity));
    }

    @Transactional
    public void deleteLead(UUID affiliateId, UUID id) {
        LeadEntity entity = requireOwnedLead(affiliateId, id);
        leadRepository.delete(entity);
        log.info("Lead deletado pelo afiliado {}: id={}", affiliateId, id);
    }

    private LeadEntity requireOwnedLead(UUID affiliateId, UUID id) {
        LeadEntity entity = leadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lead", id));
        if (entity.getAffiliate() == null || !entity.getAffiliate().getId().equals(affiliateId)) {
            throw new AccessDeniedException("Lead não pertence ao afiliado");
        }
        return entity;
    }

    private LeadResponse toLeadResponse(LeadEntity l) {
        return new LeadResponse(
                l.getId(),
                l.getCode(),
                l.getName(),
                l.getPhone(),
                l.getEmail(),
                l.getStatus(),
                l.getScore(),
                l.getLastContactAt(),
                l.getSource(),
                l.getStage(),
                l.getOwnerUser() != null ? l.getOwnerUser().getId() : null,
                l.getOwnerUser() != null ? l.getOwnerUser().getName() : null,
                l.getUpdatedAt()
        );
    }

    // -----------------------------------------------------------------
    // INTERACTIONS
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<InteractionResponse> listInteractions(UUID affiliateId, String q, Pageable pageable) {
        return interactionRepository.searchByAffiliate(affiliateId, q, pageable).map(this::toInteractionResponse);
    }

    @Transactional
    public InteractionResponse createInteraction(UUID affiliateId, InteractionCreateRequest req) {
        if (req.leadId() == null) {
            throw new IllegalArgumentException("Interação precisa estar vinculada a um lead.");
        }
        LeadEntity lead = requireOwnedLead(affiliateId, req.leadId());

        InteractionEntity entity = InteractionEntity.builder()
                .contactName(req.contactName().trim())
                .channel(req.channel() == null ? InteractionChannel.WHATSAPP : req.channel())
                .city(req.city().trim())
                .status(req.status() == null ? InteractionStatus.WAITING : req.status())
                .lead(lead)
                .build();

        InteractionEntity saved = interactionRepository.save(entity);
        log.info("Interação criada pelo afiliado {}: id={}", affiliateId, saved.getId());
        return toInteractionResponse(saved);
    }

    @Transactional
    public InteractionResponse updateInteraction(UUID affiliateId, UUID id, InteractionUpdateRequest req) {
        InteractionEntity entity = requireOwnedInteraction(affiliateId, id);

        if (req.contactName() != null && !req.contactName().isBlank()) entity.setContactName(req.contactName().trim());
        if (req.channel() != null) entity.setChannel(req.channel());
        if (req.city() != null && !req.city().isBlank()) entity.setCity(req.city().trim());
        if (req.status() != null) entity.setStatus(req.status());

        if (req.leadId() != null) {
            LeadEntity lead = requireOwnedLead(affiliateId, req.leadId());
            entity.setLead(lead);
        }

        return toInteractionResponse(interactionRepository.save(entity));
    }

    @Transactional
    public void deleteInteraction(UUID affiliateId, UUID id) {
        InteractionEntity entity = requireOwnedInteraction(affiliateId, id);
        interactionRepository.delete(entity);
    }

    private InteractionEntity requireOwnedInteraction(UUID affiliateId, UUID id) {
        InteractionEntity entity = interactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Interação", id));
        if (entity.getLead() == null
                || entity.getLead().getAffiliate() == null
                || !entity.getLead().getAffiliate().getId().equals(affiliateId)) {
            throw new AccessDeniedException("Interação não pertence ao afiliado");
        }
        return entity;
    }

    private InteractionResponse toInteractionResponse(InteractionEntity i) {
        return new InteractionResponse(
                i.getId(),
                i.getCode(),
                i.getContactName(),
                i.getChannel(),
                i.getCity(),
                i.getStatus(),
                i.getOwnerUser() != null ? i.getOwnerUser().getId() : null,
                i.getOwnerUser() != null ? i.getOwnerUser().getName() : null,
                i.getLead() != null ? i.getLead().getId() : null,
                i.getLead() != null ? i.getLead().getCode() : null,
                i.getLastMessageAt(),
                i.getUpdatedAt()
        );
    }
}
