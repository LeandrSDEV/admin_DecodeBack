package br.com.portal.decode_api.service.security;

import br.com.portal.decode_api.config.AppSecurityProperties;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.entity.security.RefreshTokenEntity;
import br.com.portal.decode_api.exception.BusinessException;
import br.com.portal.decode_api.repository.security.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppSecurityProperties appSecurityProperties;
    private final TokenHasher tokenHasher;

    private final SecureRandom random = new SecureRandom();

    public record IssuedRefreshToken(String rawToken, RefreshTokenEntity entity) {}

    @Transactional
    public IssuedRefreshToken issue(UserEntity user, String ip, String userAgent) {
        String raw = generateRaw();
        String hash = tokenHasher.sha256(raw);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusNanos(appSecurityProperties.getRefreshExpirationMs() * 1_000_000L))
                .ip(ip)
                .userAgent(userAgent)
                .build();

        entity = refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(raw, entity);
    }

    @Transactional(readOnly = true)
    public RefreshTokenEntity validate(String rawToken) {
        String hash = tokenHasher.sha256(rawToken);
        RefreshTokenEntity entity = refreshTokenRepository.findFirstByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        if (entity.isRevoked()) throw new BusinessException("Refresh token revogado");
        if (entity.isExpired()) throw new BusinessException("Refresh token expirado");
        return entity;
    }

    @Transactional
    public void rotate(RefreshTokenEntity current, RefreshTokenEntity next) {
        current.setRevokedAt(LocalDateTime.now());
        current.setReplacedBy(next.getId());
        refreshTokenRepository.save(current);
    }

    private String generateRaw() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
