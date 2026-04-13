package br.com.portal.decode_api.security;

import br.com.portal.decode_api.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit simples em memória (WAF-ready). Para escalar horizontalmente, trocar por Redis/Bucket4j.
 */
@Component
@RequiredArgsConstructor
public class SimpleRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;

    private static class Counter {
        long windowStartMs;
        int count;
    }

    private final Map<String, Counter> perKey = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!props.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr() == null ? "-" : request.getRemoteAddr();
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();

        int limit;
        long windowMs;

        if (path.equals("/api/auth/login")) {
            limit = props.getLoginPerMinute();
            windowMs = 60_000;
        } else if (path.startsWith("/api/")) {
            limit = props.getApiPerSecond();
            windowMs = 1_000;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String key = ip + "|" + (path.equals("/api/auth/login") ? "login" : "api");
        long now = Instant.now().toEpochMilli();

        Counter c = perKey.computeIfAbsent(key, k -> {
            Counter cc = new Counter();
            cc.windowStartMs = now;
            cc.count = 0;
            return cc;
        });

        synchronized (c) {
            if (now - c.windowStartMs >= windowMs) {
                c.windowStartMs = now;
                c.count = 0;
            }
            c.count++;
            if (c.count > limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"rate_limited\",\"message\":\"Muitas requisições. Tente novamente.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
