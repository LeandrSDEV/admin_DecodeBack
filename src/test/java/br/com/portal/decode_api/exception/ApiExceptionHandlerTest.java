package br.com.portal.decode_api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleAuth_returns401() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleAuth(new BadCredentialsException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleNotFound(new EntityNotFoundException("Lead", "abc-123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("Lead");
    }

    @Test
    void handleBusiness_returns422() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleBusiness(new BusinessException("E-mail já cadastrado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).isEqualTo("E-mail já cadastrado");
    }

    @Test
    void handleUnknown_returns500_withoutExposingDetails() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleUnknown(new NullPointerException("internal detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Erro interno do servidor.");
    }
}
