package br.com.portal.decode_api.security.affiliate;

import br.com.portal.decode_api.config.AffiliateProperties;
import br.com.portal.decode_api.entity.AffiliateEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Service especifico do portal do afiliado.
 * Separado do JwtService dos admins para isolar os dominios.
 * Usa seu proprio secret (app.affiliate.jwt-secret) ou cai no mesmo do admin se nao configurado.
 */
@Service
@RequiredArgsConstructor
public class AffiliateJwtService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateJwtService.class);
    private static final String PRINCIPAL_TYPE_CLAIM = "ptype";
    private static final String PRINCIPAL_TYPE = "AFFILIATE";

    private final AffiliateProperties props;

    public String generateToken(AffiliateEntity affiliate) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + props.getJwtExpirationMs());

        return Jwts.builder()
                .subject(affiliate.getId().toString())
                .claim("email", affiliate.getEmail())
                .claim("name", affiliate.getName())
                .claim("refCode", affiliate.getRefCode())
                .claim(PRINCIPAL_TYPE_CLAIM, PRINCIPAL_TYPE)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public UUID extractAffiliateId(String token) {
        String sub = extractClaim(token, Claims::getSubject);
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isAffiliateToken(String token) {
        try {
            String ptype = extractClaim(token, c -> c.get(PRINCIPAL_TYPE_CLAIM, String.class));
            return PRINCIPAL_TYPE.equals(ptype);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Date exp = extractClaim(token, Claims::getExpiration);
            return exp != null && exp.after(new Date()) && isAffiliateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return props.getJwtExpirationMs() / 1000L;
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        String s = props.getJwtSecret() == null ? "" : props.getJwtSecret().trim();
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(s);
        } catch (IllegalArgumentException e1) {
            try {
                keyBytes = Decoders.BASE64URL.decode(s);
            } catch (IllegalArgumentException e2) {
                keyBytes = s.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (keyBytes.length < 32) {
            log.error("app.affiliate.jwt-secret fraco: precisa >= 32 bytes. Atual: {} bytes", keyBytes.length);
            throw new IllegalStateException("app.affiliate.jwt-secret invalido (< 256 bits).");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
