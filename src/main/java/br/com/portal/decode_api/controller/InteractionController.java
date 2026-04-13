package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @GetMapping
    public Page<InteractionResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return interactionService.list(q, pageable);
    }

    @GetMapping("/queue")
    public List<StageCountResponse> queueSummary() {
        return interactionService.queueSummary();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionResponse create(@Valid @RequestBody InteractionCreateRequest request) {
        return interactionService.create(request);
    }

    @PutMapping("/{id}")
    public InteractionResponse update(@PathVariable UUID id, @Valid @RequestBody InteractionUpdateRequest request) {
        return interactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        interactionService.delete(id);
    }

    @GetMapping("/{id}/messages")
    public List<InteractionMessageResponse> listMessages(@PathVariable UUID id) {
        return interactionService.listMessages(id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionMessageResponse addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody InteractionMessageCreateRequest request
    ) {
        return interactionService.addMessage(id, request);
    }
}
