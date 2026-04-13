package br.com.portal.decode_api.service.security;

import br.com.portal.decode_api.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class TokenHasher {

    private final JwtProperties jwtProperties;

    public String sha256(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // inclui secret como "pepper" para reduzir impacto de vazamento do banco.
            md.update(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao hashear token", e);
        }
    }
}
