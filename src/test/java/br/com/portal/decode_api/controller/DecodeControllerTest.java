package br.com.portal.decode_api.controller;

import br.com.portal.decode_api.dtos.DecodeResponse;
import br.com.portal.decode_api.enums.DecodeStatus;
import br.com.portal.decode_api.security.JwtService;
import br.com.portal.decode_api.service.DecodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DecodeController.class)
class DecodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DecodeService decodeService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void list_returns200WithPaginatedContent() throws Exception {
        DecodeResponse decode = new DecodeResponse(
                UUID.randomUUID(), "D-001", "Decode Teste", "Aracaju",
                DecodeStatus.ACTIVE, 10, BigDecimal.valueOf(5000), null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(decodeService.list(any(), any()))
                .thenReturn(new PageImpl<>(List.of(decode), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/decodes").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Decode Teste"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        DecodeResponse decode = new DecodeResponse(
                UUID.randomUUID(), "D-002", "Novo Decode", "Salvador",
                DecodeStatus.ACTIVE, 5, BigDecimal.TEN, UUID.randomUUID(), "Afiliado X", null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(decodeService.create(any())).thenReturn(decode);

        mockMvc.perform(post("/api/decodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Novo Decode", "city": "Salvador", "affiliateId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novo Decode"));
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/decodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "city": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/decodes/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
