package br.com.portal.decode_api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    private long refreshExpirationMs = 2592000000L;

    private Sse sse = new Sse();

    @Data
    public static class Sse {
        private boolean allowQueryToken = true;
    }
}
