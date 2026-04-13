package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role
) {
}