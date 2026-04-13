package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 2, max = 120) String name,
        @Email @Size(max = 150) String email,
        UserRole role,
        Boolean active,
        @Size(min = 6, max = 72) String password
) {
}
