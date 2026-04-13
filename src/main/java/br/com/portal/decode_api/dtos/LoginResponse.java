package br.com.portal.decode_api.dtos;

public record LoginResponse(
        String token,
        String refreshToken,
        UserResponse user
) {
}