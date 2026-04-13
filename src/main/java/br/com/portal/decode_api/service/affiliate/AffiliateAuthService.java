package br.com.portal.decode_api.service.affiliate;

import br.com.portal.decode_api.dtos.affiliate.AffiliateLoginRequest;
import br.com.portal.decode_api.dtos.affiliate.AffiliateLoginResponse;
import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.repository.AffiliateRepository;
import br.com.portal.decode_api.security.affiliate.AffiliateJwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AffiliateAuthService {

    private static final Logger log = LoggerFactory.getLogger(AffiliateAuthService.class);

    private final AffiliateRepository affiliateRepository;
    private final PasswordEncoder passwordEncoder;
    private final AffiliateJwtService jwtService;

    @Transactional
    public AffiliateLoginResponse login(AffiliateLoginRequest req) {
        AffiliateEntity a = affiliateRepository.findByEmailIgnoreCase(req.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        if (a.getPasswordHash() == null) {
            throw new BadCredentialsException("Cadastro sem senha definida. Aguarde aprovacao ou contate o suporte.");
        }
        if (a.getStatus() == AffiliateStatus.PENDING) {
            throw new BadCredentialsException("Seu cadastro ainda esta em analise.");
        }
        if (a.getStatus() != AffiliateStatus.ACTIVE) {
            throw new BadCredentialsException("Acesso negado: afiliado " + a.getStatus());
        }
        if (!passwordEncoder.matches(req.password(), a.getPasswordHash())) {
            log.warn("Tentativa de login falha para afiliado {}", a.getEmail());
            throw new BadCredentialsException("Credenciais invalidas");
        }

        a.setLastLoginAt(LocalDateTime.now());
        affiliateRepository.save(a);

        String token = jwtService.generateToken(a);
        return new AffiliateLoginResponse(
                token,
                jwtService.getExpirationSeconds(),
                a.getId(),
                a.getName(),
                a.getEmail(),
                a.getRefCode(),
                a.getStatus(),
                Boolean.TRUE.equals(a.getMustChangePw())
        );
    }

    @Transactional
    public void changePassword(AffiliateEntity affiliate, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Nova senha precisa ter pelo menos 8 caracteres.");
        }
        if (!Boolean.TRUE.equals(affiliate.getMustChangePw())) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, affiliate.getPasswordHash())) {
                throw new BadCredentialsException("Senha atual incorreta.");
            }
        }
        affiliate.setPasswordHash(passwordEncoder.encode(newPassword));
        affiliate.setMustChangePw(false);
        affiliateRepository.save(affiliate);
        log.info("Senha alterada para afiliado {}", affiliate.getEmail());
    }

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String msg) { super(msg); }
    }
}
