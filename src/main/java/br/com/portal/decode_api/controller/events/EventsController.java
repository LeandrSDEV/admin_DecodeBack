package br.com.portal.decode_api.controller.events;

import br.com.portal.decode_api.dtos.events.EventIngestRequest;
import br.com.portal.decode_api.dtos.events.EventResponse;
import br.com.portal.decode_api.service.events.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventsController {

    private final EventService eventService;

    @PostMapping
    public EventResponse ingest(@Valid @RequestBody EventIngestRequest request,
                               Authentication auth,
                               HttpServletRequest http) {
        String actor = auth != null ? auth.getName() : null;
        return eventService.ingest(request, actor, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @GetMapping
    public List<EventResponse> list(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String leadId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) Long afterEventId
    ) {
        return eventService.list(from, to, type, channel, severity, leadId, limit, afterEventId);
    }
}
