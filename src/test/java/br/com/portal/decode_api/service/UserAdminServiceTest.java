package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.UserCreateRequest;
import br.com.portal.decode_api.dtos.UserRowResponse;
import br.com.portal.decode_api.dtos.UserUpdateRequest;
import br.com.portal.decode_api.entity.UserEntity;
import br.com.portal.decode_api.enums.UserRole;
import br.com.portal.decode_api.exception.BusinessException;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAdminService userAdminService;

    private UserEntity makeEntity() {
        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setName("Admin");
        u.setEmail("admin@decode.com");
        u.setPasswordHash("$2a$10$hash");
        u.setRole(UserRole.ADMIN);
        u.setActive(true);
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    @Test
    void create_newUser_returnsWithTemporaryPassword() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserCreateRequest req = new UserCreateRequest("Novo User", "novo@decode.com", UserRole.OPERATOR, null, null);
        UserRowResponse response = userAdminService.create(req);

        assertThat(response.name()).isEqualTo("Novo User");
        assertThat(response.temporaryPassword()).isNotNull();
        assertThat(response.temporaryPassword()).hasSizeGreaterThanOrEqualTo(12);
        verify(userRepository).save(any());
    }

    @Test
    void create_duplicateEmail_throwsBusinessException() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(makeEntity()));

        UserCreateRequest req = new UserCreateRequest("Dup", "admin@decode.com", null, null, null);
        assertThatThrownBy(() -> userAdminService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("E-mail já cadastrado");
    }

    @Test
    void update_nonExistent_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.update(id, new UserUpdateRequest("x", null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_existingUser_updatesName() {
        UserEntity entity = makeEntity();
        UUID id = entity.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.save(any())).thenReturn(entity);

        UserRowResponse response = userAdminService.update(id, new UserUpdateRequest("Novo Nome", null, null, null, null));

        assertThat(response).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    void delete_nonExistent_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userAdminService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
