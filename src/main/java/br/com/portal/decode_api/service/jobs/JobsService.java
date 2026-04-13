package br.com.portal.decode_api.service.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JobsService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public record JobRow(
            String id,
            String queue,
            String name,
            String status,
            Map<String, Object> payload,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public List<JobRow> list(String queue, String status, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id, queue, name, status, payload, created_at, updated_at FROM jobs WHERE 1=1");
        Map<String, Object> p = new HashMap<>();
        if (queue != null && !queue.isBlank()) {
            sql.append(" AND queue = :queue");
            p.put("queue", queue);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            p.put("status", status);
        }
        sql.append(" ORDER BY created_at DESC LIMIT :limit");
        p.put("limit", Math.min(Math.max(limit, 1), 500));

        return jdbc.query(sql.toString(), p, (rs, i) -> {
            Map<String, Object> payload = null;
            String pj = rs.getString("payload");
            if (pj != null) {
                try { payload = objectMapper.readValue(pj, Map.class); } catch (Exception ignored) {}
            }
            return new JobRow(
                    rs.getString("id"),
                    rs.getString("queue"),
                    rs.getString("name"),
                    rs.getString("status"),
                    payload,
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            );
        });
    }
}
