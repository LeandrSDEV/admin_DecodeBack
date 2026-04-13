package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.LeadCreateRequest;
import br.com.portal.decode_api.dtos.LeadResponse;
import br.com.portal.decode_api.dtos.LeadUpdateRequest;
import br.com.portal.decode_api.dtos.StageCountResponse;
import br.com.portal.decode_api.entity.LeadEntity;
import br.com.portal.decode_api.enums.LeadSource;
import br.com.portal.decode_api.enums.LeadStage;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.LeadRepository;
import br.com.portal.decode_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeadService leadService;

    private LeadEntity makeEntity() {
        LeadEntity e = new LeadEntity();
        e.setId(UUID.randomUUID());
        e.setCode("L-001");
        e.setName("Lead Teste");
        e.setPhone("79999999999");
        e.setEmail("lead@test.com");
        e.setStatus("ATIVO");
        e.setScore(50);
        e.setSource(LeadSource.WHATSAPP);
        e.setStage(LeadStage.WAITING);
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void list_returnsPageOfLeads() {
        LeadEntity entity = makeEntity();
        Pageable pageable = PageRequest.of(0, 20);
        when(leadRepository.search(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<LeadResponse> result = leadService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Lead Teste");
    }

    @Test
    void create_withoutOwner_createsSuccessfully() {
        LeadEntity saved = makeEntity();
        when(leadRepository.save(any())).thenReturn(saved);

        LeadCreateRequest req = new LeadCreateRequest("Novo Lead", "11999", "a@b.com", null, null, null, null, null);
        LeadResponse response = leadService.create(req);

        assertThat(response.name()).isEqualTo("Lead Teste");
        verify(leadRepository).save(any());
    }

    @Test
    void create_withInvalidOwner_throwsEntityNotFound() {
        UUID ownerId = UUID.randomUUID();
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        LeadCreateRequest req = new LeadCreateRequest("Lead", null, null, null, null, null, null, ownerId);
        assertThatThrownBy(() -> leadService.create(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_existingLead_updatesSuccessfully() {
        LeadEntity entity = makeEntity();
        UUID id = entity.getId();
        when(leadRepository.findById(id)).thenReturn(Optional.of(entity));
        when(leadRepository.save(any())).thenReturn(entity);

        LeadUpdateRequest req = new LeadUpdateRequest("Updated", null, null, null, 80, null, null, null);
        LeadResponse response = leadService.update(id, req);

        assertThat(response).isNotNull();
        verify(leadRepository).save(any());
    }

    @Test
    void delete_nonExistent_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(leadRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> leadService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void pipeline_returnsThreeStages() {
        when(leadRepository.countByStage(LeadStage.WAITING)).thenReturn(10L);
        when(leadRepository.countByStage(LeadStage.MEETING)).thenReturn(5L);
        when(leadRepository.countByStage(LeadStage.PROPOSAL)).thenReturn(3L);

        List<StageCountResponse> result = leadService.pipeline();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).value()).isEqualTo(10);
        assertThat(result.get(1).value()).isEqualTo(5);
        assertThat(result.get(2).value()).isEqualTo(3);
    }
}
