package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.DecodeRequest;
import br.com.portal.decode_api.dtos.DecodeResponse;
import br.com.portal.decode_api.entity.DecodeEntity;
import br.com.portal.decode_api.enums.DecodeStatus;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.DecodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecodeServiceTest {

    @Mock
    private DecodeRepository decodeRepository;

    @InjectMocks
    private DecodeService decodeService;

    private DecodeEntity makeEntity() {
        DecodeEntity e = new DecodeEntity();
        e.setId(UUID.randomUUID());
        e.setCode("D-001");
        e.setName("Decode Teste");
        e.setCity("Aracaju");
        e.setStatus(DecodeStatus.ACTIVE);
        e.setUsersCount(10);
        e.setMonthlyRevenue(BigDecimal.valueOf(5000));
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void list_returnsPageOfDecodes() {
        DecodeEntity entity = makeEntity();
        Pageable pageable = PageRequest.of(0, 20);
        when(decodeRepository.search(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<DecodeResponse> result = decodeService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Decode Teste");
    }

    @Test
    void create_savesAndReturnsResponse() {
        DecodeEntity saved = makeEntity();
        when(decodeRepository.save(any())).thenReturn(saved);

        DecodeRequest req = new DecodeRequest("Novo Decode", "Salvador", DecodeStatus.ACTIVE, 5, BigDecimal.TEN, null, null);
        DecodeResponse response = decodeService.create(req);

        assertThat(response.name()).isEqualTo("Decode Teste");
        verify(decodeRepository).save(any());
    }

    @Test
    void update_existingEntity_updatesSuccessfully() {
        DecodeEntity entity = makeEntity();
        UUID id = entity.getId();
        when(decodeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(decodeRepository.save(any())).thenReturn(entity);

        DecodeRequest req = new DecodeRequest("Updated", "Recife", null, null, null, null, null);
        DecodeResponse response = decodeService.update(id, req);

        assertThat(response).isNotNull();
        verify(decodeRepository).findById(id);
        verify(decodeRepository).save(any());
    }

    @Test
    void update_nonExistentEntity_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(decodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> decodeService.update(id, new DecodeRequest("x", "y", null, null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existingEntity_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        when(decodeRepository.existsById(id)).thenReturn(true);

        decodeService.delete(id);

        verify(decodeRepository).deleteById(id);
    }

    @Test
    void delete_nonExistentEntity_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(decodeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> decodeService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
