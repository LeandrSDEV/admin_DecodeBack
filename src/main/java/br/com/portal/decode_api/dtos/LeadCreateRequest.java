package br.com.portal.decode_api.dtos;

import br.com.portal.decode_api.enums.LeadSource;
import br.com.portal.decode_api.enums.LeadStage;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record LeadCreateRequest(
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        String status,
        @Min(0) @Max(100) Integer score,
        LeadSource source,
        LeadStage stage,
        UUID ownerUserId
) {
}
