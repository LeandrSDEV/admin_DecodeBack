package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.DecodeRequest;
import br.com.portal.decode_api.dtos.DecodeResponse;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.enums.DecodeStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.repository.DecodeRepository;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DecodeService {

    private static final Logger log = LoggerFactory.getLogger(DecodeService.class);

    private final DecodeRepository decodeRepository;
    private final AffiliateRepository affiliateRepository;

    @Transactional(readOnly = true)
    public Page<DecodeResponse> list(String q, Pageable pageable) {
        return decodeRepository.search(q, pageable).map(this::toResponse);
    }

    @Transactional
    public DecodeResponse create(DecodeRequest req) {
        if (req.affiliateId() == null) {
            throw new IllegalArgumentException("Afiliado responsavel pela conversao e obrigatorio.");
        }
        AffiliateEntity affiliate = affiliateRepository.findById(req.affiliateId())
                .orElseThrow(() -> new EntityNotFoundException("Affiliate", req.affiliateId()));

        DecodeEntity entity = DecodeEntity.builder()
                .name(req.name().trim())
                .city(req.city().trim())
                .status(req.status() == null ? DecodeStatus.ACTIVE : req.status())
                .usersCount(req.usersCount())
                .monthlyRevenue(req.monthlyRevenue())
                .affiliate(affiliate)
                .affiliateAttachedAt(LocalDateTime.now())
                .build();

        DecodeEntity saved = decodeRepository.save(entity);
        log.info("Decode criado: id={}, code={}, afiliado={}", saved.getId(), saved.getCode(), affiliate.getId());
        return toResponse(saved);
    }

    @Transactional
    public DecodeResponse update(UUID id, DecodeRequest req) {
        DecodeEntity entity = decodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Decode", id));

        entity.setName(req.name().trim());
        entity.setCity(req.city().trim());
        if (req.status() != null) entity.setStatus(req.status());
        if (req.usersCount() != null) entity.setUsersCount(req.usersCount());
        if (req.monthlyRevenue() != null) entity.setMonthlyRevenue(req.monthlyRevenue());

        if (req.affiliateId() != null) {
            UUID currentAffiliateId = entity.getAffiliate() != null ? entity.getAffiliate().getId() : null;
            if (!req.affiliateId().equals(currentAffiliateId)) {
                AffiliateEntity affiliate = affiliateRepository.findById(req.affiliateId())
                        .orElseThrow(() -> new EntityNotFoundException("Affiliate", req.affiliateId()));
                entity.setAffiliate(affiliate);
                entity.setAffiliateAttachedAt(LocalDateTime.now());
            }
        }

        log.info("Decode atualizado: id={}, afiliado={}", id,
                entity.getAffiliate() != null ? entity.getAffiliate().getId() : null);
        return toResponse(decodeRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!decodeRepository.existsById(id)) {
            throw new EntityNotFoundException("Decode", id);
        }
        decodeRepository.deleteById(id);
        log.info("Decode deletado: id={}", id);
    }

    private DecodeResponse toResponse(DecodeEntity d) {
        AffiliateEntity aff = d.getAffiliate();
        return new DecodeResponse(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getCity(),
                d.getStatus(),
                d.getUsersCount(),
                d.getMonthlyRevenue(),
                aff != null ? aff.getId() : null,
                aff != null ? aff.getName() : null,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
