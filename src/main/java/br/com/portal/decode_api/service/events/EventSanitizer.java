package br.com.portal.decode_api.service.events;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EventSanitizer {

    private static final int MAX_JSON_CHARS = 8_000;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "senha", "token", "access_token", "refresh_token",
            "authorization", "auth", "secret", "api_key", "apikey",
            "cpf", "cnpj", "rg", "creditcard", "card_number", "cvv"
    );

    public Map<String, Object> sanitize(Map<String, Object> payload) {
        if (payload == null) return null;
        Map<String, Object> copy = deepCopy(payload, 0);
        // tamanho final controlado no serviço (string), aqui é estrutural.
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> src, int depth) {
        if (src == null) return null;
        if (depth > 8) return Map.of("_truncated", true);

        Map<String, Object> out = new LinkedHashMap<>();
        for (var e : src.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();

            if (k != null && SENSITIVE_KEYS.contains(k.toLowerCase())) {
                out.put(k, "***");
                continue;
            }

            if (v instanceof Map<?, ?> m) {
                out.put(k, deepCopy((Map<String, Object>) m, depth + 1));
            } else if (v instanceof List<?> list) {
                out.put(k, sanitizeList(list, depth + 1));
            } else if (v instanceof String s) {
                out.put(k, s.length() > 2000 ? s.substring(0, 2000) + "…" : s);
            } else {
                out.put(k, v);
            }
        }
        return out;
    }

    private Object sanitizeList(List<?> list, int depth) {
        if (depth > 8) return List.of("_truncated");
        List<Object> out = new ArrayList<>();
        int cap = Math.min(list.size(), 50);
        for (int i = 0; i < cap; i++) {
            Object v = list.get(i);
            if (v instanceof Map<?, ?> m) out.add(deepCopy((Map<String, Object>) m, depth + 1));
            else out.add(v);
        }
        if (list.size() > cap) out.add("…");
        return out;
    }

    public int maxJsonChars() {
        return MAX_JSON_CHARS;
    }
}
