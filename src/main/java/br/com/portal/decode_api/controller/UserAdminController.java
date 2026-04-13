package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.UserCreateRequest;
import br.com.portal.decode_api.dtos.UserRowResponse;
import br.com.portal.decode_api.dtos.UserUpdateRequest;
import br.com.portal.decode_api.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public Page<UserRowResponse> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return userAdminService.list(q, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRowResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userAdminService.create(request);
    }

    @PutMapping("/{id}")
    public UserRowResponse update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return userAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userAdminService.delete(id);
    }
}
