package br.com.portal.decode_api.repository.security;

import br.com.portal.decode_api.entity.security.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findFirstByTokenHash(String tokenHash);
}
