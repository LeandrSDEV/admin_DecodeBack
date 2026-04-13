package br.com.portal.decode_api.security.affiliate;

import br.com.portal.decode_api.entity.AffiliateEntity;
import lombok.Getter;

import java.util.UUID;

/**
 * Principal injetado no SecurityContext quando um afiliado faz request autenticada no portal.
 */
@Getter
public class AffiliatePrincipal {
    private final UUID id;
    private final String email;
    private final String name;
    private final String refCode;

    public AffiliatePrincipal(AffiliateEntity affiliate) {
        this.id = affiliate.getId();
        this.email = affiliate.getEmail();
        this.name = affiliate.getName();
        this.refCode = affiliate.getRefCode();
    }

    @Override
    public String toString() {
        return "AffiliatePrincipal[" + email + "]";
    }
}
