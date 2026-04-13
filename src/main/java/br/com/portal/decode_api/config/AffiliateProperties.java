package br.com.portal.decode_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.affiliate")
@Getter
@Setter
public class AffiliateProperties {

    /** Taxa base aplicada no primeiro tier (default 15%). */
    private BigDecimal baseRate = new BigDecimal("15.00");

    /** Taxa aplicada quando o afiliado atinge o bonusThreshold no mes (default 18%). */
    private BigDecimal bonusRate = new BigDecimal("18.00");

    /** Quantidade de novas conversoes no mes que dispara o bonusRate (default 5). */
    private int bonusThreshold = 5;

    /** Meses de carencia antes da comissao ser aprovada para pagamento (default 2). */
    private int carenciaMonths = 2;

    /** JWT secret especifico do portal do afiliado (se nulo, usa o mesmo do admin). */
    private String jwtSecret;

    /** TTL do JWT do portal em ms (default 24h). */
    private long jwtExpirationMs = 86_400_000L;

    /** URL base do portal publico do afiliado, usada em emails e share links. */
    private String portalBaseUrl = "https://decodeapp.com/afiliado";

    /** URL base da landing publica com o parametro ?ref= pra tracking. */
    private String landingBaseUrl = "https://gestao-decode.lovable.app";
}
