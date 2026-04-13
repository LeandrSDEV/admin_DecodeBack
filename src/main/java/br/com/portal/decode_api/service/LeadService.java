package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.LeadCreateRequest;
import br.com.portal.decode_api.dtos.LeadResponse;
import br.com.portal.decode_api.dtos.LeadUpdateRequest;
import br.com.portal.decode_api.dtos.StageCountResponse;
import br.com.portal.decode_api.entity.LeadEntity;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.LeadSource;
import br.com.portal.decode_api.enums.LeadStage;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.LeadRepository;
import br.com.portal.decode_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<LeadResponse> list(String q, Pageable pageable) {
        return leadRepository.search(q, pageable).map(this::toResponse);
    }

    @Transactional
    public LeadResponse create(LeadCreateRequest req) {
        UserEntity owner = null;
        if (req.ownerUserId() != null) {
            owner = userRepository.findById(req.ownerUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuário (owner)", req.ownerUserId()));
        }

        LeadEntity entity = LeadEntity.builder()
                .name(req.name().trim())
                .phone(req.phone())
                .email(req.email())
                .status(req.status())
                .score(req.score() == null ? 0 : req.score())
                .source(req.source() == null ? LeadSource.WHATSAPP : req.source())
                .stage(req.stage() == null ? LeadStage.WAITING : req.stage())
                .ownerUser(owner)
                .build();

        LeadEntity saved = leadRepository.save(entity);
        log.info("Lead criado: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Transactional
    public LeadResponse update(UUID id, LeadUpdateRequest req) {
        LeadEntity entity = leadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lead", id));

        if (req.name() != null && !req.name().isBlank()) entity.setName(req.name().trim());
        if (req.phone() != null) entity.setPhone(req.phone());
        if (req.email() != null) entity.setEmail(req.email());
        if (req.status() != null) entity.setStatus(req.status());
        if (req.score() != null) entity.setScore(req.score());
        if (req.source() != null) entity.setSource(req.source());
        if (req.stage() != null) entity.setStage(req.stage());

        if (req.ownerUserId() != null) {
            UserEntity owner = userRepository.findById(req.ownerUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuário (owner)", req.ownerUserId()));
            entity.setOwnerUser(owner);
        }

        log.info("Lead atualizado: id={}", id);
        return toResponse(leadRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!leadRepository.existsById(id)) throw new EntityNotFoundException("Lead", id);
        leadRepository.deleteById(id);
        log.info("Lead deletado: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<StageCountResponse> pipeline() {
        return List.of(
                new StageCountResponse("Aguard.", leadRepository.countByStage(LeadStage.WAITING)),
                new StageCountResponse("Reunião", leadRepository.countByStage(LeadStage.MEETING)),
                new StageCountResponse("Proposta", leadRepository.countByStage(LeadStage.PROPOSAL))
        );
    }

    private LeadResponse toResponse(LeadEntity l) {
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
}
