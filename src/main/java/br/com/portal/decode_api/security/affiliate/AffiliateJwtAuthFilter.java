package br.com.portal.decode_api.security.affiliate;

import br.com.portal.decode_api.entity.AffiliateEntity;
import br.com.portal.decode_api.enums.AffiliateStatus;
import br.com.portal.decode_api.repository.AffiliateRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Filtro JWT do portal do afiliado. Registrado apenas na filter chain de /api/affiliate/**.
 */
@Component
@RequiredArgsConstructor
public class AffiliateJwtAuthFilter extends OncePerRequestFilter {

    private final AffiliateJwtService jwtService;
    private final AffiliateRepository affiliateRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtService.isAffiliateToken(token) || !jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID affiliateId = jwtService.extractAffiliateId(token);
            if (affiliateId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<AffiliateEntity> opt = affiliateRepository.findById(affiliateId);
                if (opt.isPresent() && opt.get().getStatus() == AffiliateStatus.ACTIVE) {
                    AffiliatePrincipal principal = new AffiliatePrincipal(opt.get());
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_AFFILIATE"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
        }

        filterChain.doFilter(request, response);
    }
}
