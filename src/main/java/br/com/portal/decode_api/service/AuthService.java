package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.LoginRequest;
import br.com.portal.decode_api.dtos.LoginResponse;
import br.com.portal.decode_api.dtos.UserResponse;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.UserRepository;
import br.com.portal.decode_api.security.JwtService;
import br.com.portal.decode_api.service.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserEntity user = userRepository.findByEmailIgnoreCaseAndActiveTrue(request.email())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String token = jwtService.generateToken(user);
        var issued = refreshTokenService.issue(user, ip, userAgent);

        log.info("Login bem-sucedido: email={}, ip={}", request.email(), ip);

        return new LoginResponse(
                token,
                issued.rawToken(),
                new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole())
        );
    }

    @Transactional
    public LoginResponse refresh(String refreshToken, String ip, String userAgent) {
        var current = refreshTokenService.validate(refreshToken);
        UserEntity user = current.getUser();

        String token = jwtService.generateToken(user);
        var issued = refreshTokenService.issue(user, ip, userAgent);
        refreshTokenService.rotate(current, issued.entity());

        log.debug("Token refresh: userId={}", user.getId());

        return new LoginResponse(
                token,
                issued.rawToken(),
                new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole())
        );
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
