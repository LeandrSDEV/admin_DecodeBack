package br.com.portal.decode_api.service.events;

import br.com.portal.decode_api.dtos.events.EventIngestRequest;
import br.com.portal.decode_api.dtos.events.EventResponse;
import br.com.portal.decode_api.dtos.events.KpisResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final EventSanitizer sanitizer;
    private final SseHub sseHub;
    private final StringRedisTemplate redis;

    public EventResponse ingest(EventIngestRequest req, String actorUserId, String ip, String userAgent) {
        LocalDateTime occurredAt = req.occurredAt() != null ? req.occurredAt() : LocalDateTime.now();
        String severity = (req.severity() == null || req.severity().isBlank()) ? "INFO" : req.severity().toUpperCase();

        Map<String, Object> sanitized = sanitizer.sanitize(req.payload());
        String payloadJson = null;
        try {
            if (sanitized != null) {
                payloadJson = objectMapper.writeValueAsString(sanitized);
                if (payloadJson.length() > sanitizer.maxJsonChars()) {
                    payloadJson = payloadJson.substring(0, sanitizer.maxJsonChars()) + "\"}";
                }
            }
        } catch (Exception ignored) {
            payloadJson = null;
        }

        var params = new MapSqlParameterSource()
                .addValue("type", req.type())
                .addValue("occurred_at", occurredAt)
                .addValue("lead_id", req.leadId() != null ? req.leadId().toString() : null)
                .addValue("channel", req.channel())
                .addValue("source", req.source())
                .addValue("payload", payloadJson)
                .addValue("actor_user_id", actorUserId)
                .addValue("trace_id", req.traceId())
                .addValue("severity", severity)
                .addValue("ip", ip)
                .addValue("user_agent", userAgent);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO events(type, occurred_at, lead_id, channel, source, payload, actor_user_id, trace_id, severity, ip, user_agent) " +
                        "VALUES (:type, :occurred_at, :lead_id, :channel, :source, CAST(:payload AS jsonb), :actor_user_id, :trace_id, :severity, :ip, :user_agent)",
                params,
                keyHolder,
                new String[] { "event_id" }
        );
        Long eventId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : -1L;

        Map<String, Object> payloadMap = null;
        if (payloadJson != null) {
            try {
                payloadMap = objectMapper.readValue(payloadJson, new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }

        EventResponse out = new EventResponse(
                eventId == null ? -1 : eventId,
                req.type(),
                occurredAt,
                req.leadId() != null ? req.leadId().toString() : null,
                req.channel(),
                req.source(),
                payloadMap,
                actorUserId,
                req.traceId(),
                severity
        );

        // invalida cache curto
        try {
            redis.opsForValue().set("kpi:dirty", "1", Duration.ofSeconds(2));
        } catch (Exception ignored) {
        }

        // broadcast delta
        sseHub.broadcast("event", out);

        return out;
    }

    public List<EventResponse> list(LocalDateTime from, LocalDateTime to, String type, String channel, String severity,
                                   String leadId, int limit, Long afterEventId) {

        StringBuilder sql = new StringBuilder(
                "SELECT event_id, type, occurred_at, lead_id, channel, source, payload, actor_user_id, trace_id, severity " +
                        "FROM events WHERE 1=1 "
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
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = :type");
            p.put("type", type);
        }
        if (channel != null && !channel.isBlank()) {
            sql.append(" AND channel = :channel");
            p.put("channel", channel);
        }
        if (severity != null && !severity.isBlank()) {
            sql.append(" AND severity = :severity");
            p.put("severity", severity.toUpperCase());
        }
        if (leadId != null && !leadId.isBlank()) {
            sql.append(" AND lead_id = :lead_id");
            p.put("lead_id", leadId);
        }
        if (afterEventId != null && afterEventId > 0) {
            sql.append(" AND event_id > :after");
            p.put("after", afterEventId);
            sql.append(" ORDER BY event_id ASC ");
        } else {
            sql.append(" ORDER BY occurred_at DESC, event_id DESC ");
        }

        int capped = Math.min(Math.max(limit, 1), 500);
        sql.append(" LIMIT :limit");
        p.put("limit", capped);

        return jdbc.query(sql.toString(), p, (rs, i) -> {
            String payloadJson = rs.getString("payload");
            Map<String, Object> payload = null;
            if (payloadJson != null) {
                try {
                    payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});
                } catch (Exception ignored) {
                }
            }

            return new EventResponse(
                    rs.getLong("event_id"),
                    rs.getString("type"),
                    rs.getTimestamp("occurred_at").toLocalDateTime(),
                    rs.getString("lead_id"),
                    rs.getString("channel"),
                    rs.getString("source"),
                    payload,
                    rs.getString("actor_user_id"),
                    rs.getString("trace_id"),
                    rs.getString("severity")
            );
        });
    }

    public KpisResponse kpis(String window) {
        String win = (window == null || window.isBlank()) ? "5m" : window;
        Duration dur = switch (win) {
            case "1h" -> Duration.ofHours(1);
            case "24h" -> Duration.ofHours(24);
            default -> Duration.ofMinutes(5);
        };
        LocalDateTime from = LocalDateTime.now().minus(dur);

        // cache redis 1s (graceful when Redis is unavailable)
        String cacheKey = "kpi:" + win;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, KpisResponse.class);
            }
        } catch (Exception ignored) {
        }

        Map<String, Object> p = Map.of("from", from);

        Map<String, Object> base = jdbc.queryForMap(
                "SELECT COUNT(*) AS total, " +
                        "SUM(CASE WHEN type='MESSAGE_IN' THEN 1 ELSE 0 END) AS messages_in, " +
                        "SUM(CASE WHEN type='MESSAGE_OUT' THEN 1 ELSE 0 END) AS messages_out, " +
                        "SUM(CASE WHEN type='LEAD_CREATED' THEN 1 ELSE 0 END) AS leads_new, " +
                        "SUM(CASE WHEN severity IN ('ERROR','FATAL') OR type IN ('JOB_FAILED','API_ERROR') THEN 1 ELSE 0 END) AS failures " +
                        "FROM events WHERE occurred_at >= :from",
                p
        );

        long total = toLong(base.get("total"));
        long messagesIn = toLong(base.get("messages_in"));
        long messagesOut = toLong(base.get("messages_out"));
        long leadsNew = toLong(base.get("leads_new"));
        long failures = toLong(base.get("failures"));

        double eps = total / (double) Math.max(1, dur.toSeconds());

        // latency metrics from payload.latency_ms when present (PostgreSQL JSONB)
        Double avgLatency = jdbc.queryForObject(
                "SELECT AVG((payload->>'latency_ms')::double precision) " +
                        "FROM events WHERE occurred_at >= :from AND payload ? 'latency_ms'",
                p,
                Double.class
        );

        // PostgreSQL supports percentile_cont natively
        Double p95Latency = jdbc.queryForObject(
                "SELECT percentile_cont(0.95) WITHIN GROUP (ORDER BY (payload->>'latency_ms')::double precision) " +
                        "FROM events WHERE occurred_at >= :from AND payload ? 'latency_ms'",
                p,
                Double.class
        );

        // eps series (bucket 1s)
        List<Map<String, Object>> epsSeries = jdbc.query(
                "SELECT EXTRACT(EPOCH FROM date_trunc('second', occurred_at))::bigint AS t, COUNT(*) AS v " +
                        "FROM events WHERE occurred_at >= :from GROUP BY t ORDER BY t",
                p,
                (rs, i) -> Map.of(
                        "t", rs.getLong("t"),
                        "v", rs.getLong("v")
                )
        );

        List<Map<String, Object>> byChannel = jdbc.query(
                "SELECT COALESCE(channel,'-') AS k, COUNT(*) AS v " +
                        "FROM events WHERE occurred_at >= :from GROUP BY 1 ORDER BY 2 DESC LIMIT 12",
                p,
                (rs, i) -> Map.of(
                        "k", rs.getString("k"),
                        "v", rs.getLong("v")
                )
        );

        KpisResponse out = new KpisResponse(win, eps, total, messagesIn, messagesOut, leadsNew, failures, avgLatency, p95Latency, epsSeries, byChannel);

        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(out), Duration.ofSeconds(1));
        } catch (Exception ignored) {
        }

        return out;
    }

    private static long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }
}
