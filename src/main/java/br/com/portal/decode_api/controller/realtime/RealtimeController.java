package br.com.portal.decode_api.controller.realtime;

import br.com.portal.decode_api.service.events.SseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeController {

    private final SseHub sseHub;

    @GetMapping("/stream")
    public SseEmitter stream() {
        return sseHub.register();
    }
}
