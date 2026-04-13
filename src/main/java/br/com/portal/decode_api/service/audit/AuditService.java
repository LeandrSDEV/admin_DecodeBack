package br.com.portal.decode_api.service.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public record AuditRow(
            String id,
            LocalDateTime occurredAt,
            String actorUserId,
            String action,
            String entityType,
            String entityId,
            String ip,
            String userAgent,
            String traceId,
            Map<String, Object> beforeJson,
            Map<String, Object> afterJson
    ) {}

    public List<AuditRow> list(LocalDateTime from, LocalDateTime to, String actorUserId, String action, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, occurred_at, actor_user_id, action, entity_type, entity_id, ip, user_agent, trace_id, before_json, after_json " +
                        "FROM audit_log WHERE 1=1"
        );
        Map<String, Object> p = new HashMap<>();
        if (from != null) {
            sql.append(" AND occurred_at >= :from");
            p.put("from", from);
        }
        if (to != null) {
            sql.append(" AND occurred_at <= :to");
            p.put("to", to);
        }
        if (actorUserId != null && !actorUserId.isBlank()) {
            sql.append(" AND actor_user_id = :actor");
            p.put("actor", actorUserId);
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = :action");
            p.put("action", action);
        }
        sql.append(" ORDER BY occurred_at DESC LIMIT :limit");
        p.put("limit", Math.min(Math.max(limit, 1), 500));

        return jdbc.query(sql.toString(), p, (rs, i) -> {
            Map<String, Object> before = null;
            Map<String, Object> after = null;
            String b = rs.getString("before_json");
            String a = rs.getString("after_json");
            try { if (b != null) before = objectMapper.readValue(b, new TypeReference<>() {});} catch (Exception ignored) {}
            try { if (a != null) after = objectMapper.readValue(a, new TypeReference<>() {});} catch (Exception ignored) {}
            return new AuditRow(
                    rs.getString("id"),
                    rs.getTimestamp("occurred_at").toLocalDateTime(),
                    rs.getString("actor_user_id"),
                    rs.getString("action"),
                    rs.getString("entity_type"),
                    rs.getString("entity_id"),
                    rs.getString("ip"),
                    rs.getString("user_agent"),
                    rs.getString("trace_id"),
                    before,
                    after
            );
        });
    }
}
