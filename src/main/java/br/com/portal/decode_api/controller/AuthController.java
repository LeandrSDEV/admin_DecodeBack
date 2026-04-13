package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.LoginRequest;
import br.com.portal.decode_api.dtos.LoginResponse;
import br.com.portal.decode_api.dtos.RefreshTokenRequest;
import br.com.portal.decode_api.dtos.UserResponse;
import br.com.portal.decode_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return authService.refresh(request.refreshToken(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}