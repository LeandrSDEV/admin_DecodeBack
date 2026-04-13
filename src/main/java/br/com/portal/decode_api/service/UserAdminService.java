package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.UserCreateRequest;
import br.com.portal.decode_api.dtos.UserRowResponse;
import br.com.portal.decode_api.dtos.UserUpdateRequest;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.UserRole;
import br.com.portal.decode_api.exception.BusinessException;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserRowResponse> list(String q, Pageable pageable) {
        return userRepository.search(q, pageable).map(this::toRow);
    }

    @Transactional
    public UserRowResponse create(UserCreateRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessException("E-mail já cadastrado");
        }

        String rawPass = (req.password() == null || req.password().isBlank()) ? generatePassword(12) : req.password();

        UserEntity entity = UserEntity.builder()
                .id(UUID.randomUUID())
                .name(req.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPass))
                .role(req.role() == null ? UserRole.OPERATOR : req.role())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .build();

        UserEntity saved = userRepository.save(entity);
        log.info("Usuário criado: id={}, email={}", saved.getId(), saved.getEmail());

        return new UserRowResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole(),
                Boolean.TRUE.equals(saved.getActive()) ? "Ativo" : "Bloqueado",
                rawPass
        );
    }

    @Transactional
    public UserRowResponse update(UUID id, UserUpdateRequest req) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário", id));

        if (req.name() != null && !req.name().isBlank()) entity.setName(req.name().trim());
        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmailIgnoreCase(email)
                    .filter(u -> !u.getId().equals(id))
                    .ifPresent(u -> {
                        throw new BusinessException("E-mail já cadastrado");
                    });
            entity.setEmail(email);
        }
        if (req.role() != null) entity.setRole(req.role());
        if (req.active() != null) entity.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(req.password()));
        }

        UserEntity saved = userRepository.save(entity);
        log.info("Usuário atualizado: id={}", id);
        return toRow(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) throw new EntityNotFoundException("Usuário", id);
        userRepository.deleteById(id);
        log.info("Usuário deletado: id={}", id);
    }

    private UserRowResponse toRow(UserEntity u) {
        return new UserRowResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                Boolean.TRUE.equals(u.getActive()) ? "Ativo" : "Bloqueado",
                null
        );
    }

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";

    private String generatePassword(int len) {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
