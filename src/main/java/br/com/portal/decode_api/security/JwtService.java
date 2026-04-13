package br.com.portal.decode_api.security;

import br.com.portal.decode_api.config.JwtProperties;
import br.com.portal.decode_api.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private Long jwtExpirationMs;

    public String generateToken(UserEntity user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null
                && username.equalsIgnoreCase(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractClaim(token, Claims::getExpiration);
        return exp == null || exp.before(new Date());
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
        String s = (jwtSecret == null) ? "" : jwtSecret.trim();

        byte[] keyBytes;
        String mode;

        try {
            keyBytes = Decoders.BASE64.decode(s);
            mode = "BASE64";
        } catch (IllegalArgumentException e1) {
            try {
                keyBytes = Decoders.BASE64URL.decode(s);
                mode = "BASE64URL";
            } catch (IllegalArgumentException e2) {
                keyBytes = s.getBytes(StandardCharsets.UTF_8);
                mode = "RAW_UTF8";
            }
        }

        log.debug("JWT secret mode={}, keyLength={} bytes", mode, keyBytes.length);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret fraco/inválido: precisa ter pelo menos 32 bytes (256 bits)."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
