package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.*;
import br.com.portal.decode_api.service.LeadService;
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
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public Page<LeadResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return leadService.list(q, pageable);
    }

    @GetMapping("/pipeline")
    public List<StageCountResponse> pipeline() {
        return leadService.pipeline();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse create(@Valid @RequestBody LeadCreateRequest request) {
        return leadService.create(request);
    }

    @PutMapping("/{id}")
    public LeadResponse update(@PathVariable UUID id, @Valid @RequestBody LeadUpdateRequest request) {
        return leadService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        leadService.delete(id);
    }
}
