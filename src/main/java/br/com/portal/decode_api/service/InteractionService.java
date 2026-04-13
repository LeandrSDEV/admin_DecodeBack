package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.entity.*;
import br.com.portal.decode_api.enums.InteractionChannel;
import br.com.portal.decode_api.enums.InteractionStatus;
import br.com.portal.decode_api.enums.MessageDirection;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private static final Logger log = LoggerFactory.getLogger(InteractionService.class);

    private final InteractionRepository interactionRepository;
    private final InteractionMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public Page<InteractionResponse> list(String q, Pageable pageable) {
        return interactionRepository.search(q, pageable).map(this::toResponse);
    }

    @Transactional
    public InteractionResponse create(InteractionCreateRequest req) {
        UserEntity owner = null;
        if (req.ownerUserId() != null) {
            owner = userRepository.findById(req.ownerUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Responsável", req.ownerUserId()));
        }

        LeadEntity lead = null;
        if (req.leadId() != null) {
            lead = leadRepository.findById(req.leadId())
                    .orElseThrow(() -> new EntityNotFoundException("Lead", req.leadId()));
        }

        InteractionEntity entity = InteractionEntity.builder()
                .contactName(req.contactName().trim())
                .channel(req.channel() == null ? InteractionChannel.WHATSAPP : req.channel())
                .city(req.city().trim())
                .status(req.status() == null ? InteractionStatus.WAITING : req.status())
                .ownerUser(owner)
                .lead(lead)
                .build();

        InteractionEntity saved = interactionRepository.save(entity);
        log.info("Interação criada: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Transactional
    public InteractionResponse update(UUID id, InteractionUpdateRequest req) {
        InteractionEntity entity = interactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Interação", id));

        if (req.contactName() != null && !req.contactName().isBlank()) entity.setContactName(req.contactName().trim());
        if (req.channel() != null) entity.setChannel(req.channel());
        if (req.city() != null && !req.city().isBlank()) entity.setCity(req.city().trim());
        if (req.status() != null) entity.setStatus(req.status());

        if (req.ownerUserId() != null) {
            UserEntity owner = userRepository.findById(req.ownerUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Responsável", req.ownerUserId()));
            entity.setOwnerUser(owner);
        }

        if (req.leadId() != null) {
            LeadEntity lead = leadRepository.findById(req.leadId())
                    .orElseThrow(() -> new EntityNotFoundException("Lead", req.leadId()));
            entity.setLead(lead);
        }

        log.info("Interação atualizada: id={}", id);
        return toResponse(interactionRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!interactionRepository.existsById(id)) throw new EntityNotFoundException("Interação", id);
        interactionRepository.deleteById(id);
        log.info("Interação deletada: id={}", id);
    }

    @Transactional
    public InteractionMessageResponse addMessage(UUID interactionId, InteractionMessageCreateRequest req) {
        InteractionEntity interaction = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new EntityNotFoundException("Interação", interactionId));

        LocalDateTime sentAt = req.sentAt() == null ? LocalDateTime.now() : req.sentAt();
        MessageDirection dir = req.direction() == null ? MessageDirection.OUTBOUND : req.direction();

        InteractionMessageEntity msg = InteractionMessageEntity.builder()
                .interaction(interaction)
                .direction(dir)
                .body(req.body())
                .sentAt(sentAt)
                .build();

        InteractionMessageEntity saved = messageRepository.save(msg);

        interaction.setLastMessageAt(sentAt);
        if (dir == MessageDirection.INBOUND) {
            interaction.setStatus(InteractionStatus.ANSWERED);
        } else {
            if (interaction.getStatus() == null) interaction.setStatus(InteractionStatus.WAITING);
        }
        interactionRepository.save(interaction);

        log.info("Mensagem adicionada à interação: interactionId={}, direction={}", interactionId, dir);
        return new InteractionMessageResponse(saved.getId(), interaction.getId(), saved.getDirection(), saved.getBody(), saved.getSentAt());
    }

    @Transactional(readOnly = true)
    public List<InteractionMessageResponse> listMessages(UUID interactionId) {
        return messageRepository.findLatestByInteraction(interactionId).stream()
                .map(m -> new InteractionMessageResponse(
                        m.getId(),
                        m.getInteraction().getId(),
                        m.getDirection(),
                        m.getBody(),
                        m.getSentAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StageCountResponse> queueSummary() {
        return List.of(
                new StageCountResponse("Aguardando", interactionRepository.countByStatus(InteractionStatus.WAITING)),
                new StageCountResponse("Respondido", interactionRepository.countByStatus(InteractionStatus.ANSWERED)),
                new StageCountResponse("Sem retorno", interactionRepository.countByStatus(InteractionStatus.NO_RESPONSE))
        );
    }

    private InteractionResponse toResponse(InteractionEntity i) {
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
