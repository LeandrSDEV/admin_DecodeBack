package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.UserRole;

import java.util.UUID;

public record UserRowResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        String status,
        String temporaryPassword
) {
}
