package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.DecodeRequest;
import br.com.portal.decode_api.dtos.DecodeResponse;
import br.com.portal.decode_api.service.DecodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/decodes")
@RequiredArgsConstructor
public class DecodeController {

    private final DecodeService decodeService;

    @GetMapping
    public Page<DecodeResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return decodeService.list(q, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DecodeResponse create(@Valid @RequestBody DecodeRequest request) {
        return decodeService.create(request);
    }

    @PutMapping("/{id}")
    public DecodeResponse update(@PathVariable UUID id, @Valid @RequestBody DecodeRequest request) {
        return decodeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        decodeService.delete(id);
    }
}
