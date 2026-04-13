package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.InteractionCreateRequest;
import br.com.portal.decode_api.dtos.InteractionMessageCreateRequest;
import br.com.portal.decode_api.dtos.InteractionMessageResponse;
import br.com.portal.decode_api.dtos.InteractionResponse;
import br.com.portal.decode_api.entity.InteractionEntity;
import br.com.portal.decode_api.entity.InteractionMessageEntity;
import br.com.portal.decode_api.enums.InteractionChannel;
import br.com.portal.decode_api.enums.InteractionStatus;
import br.com.portal.decode_api.enums.MessageDirection;
import br.com.portal.decode_api.exception.EntityNotFoundException;
import br.com.portal.decode_api.repository.InteractionMessageRepository;
import br.com.portal.decode_api.repository.InteractionRepository;
import br.com.portal.decode_api.repository.LeadRepository;
import br.com.portal.decode_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock private InteractionRepository interactionRepository;
    @Mock private InteractionMessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeadRepository leadRepository;

    @InjectMocks
    private InteractionService interactionService;

    private InteractionEntity makeEntity() {
        InteractionEntity e = new InteractionEntity();
        e.setId(UUID.randomUUID());
        e.setCode("I-001");
        e.setContactName("João");
        e.setChannel(InteractionChannel.WHATSAPP);
        e.setCity("Aracaju");
        e.setStatus(InteractionStatus.WAITING);
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void create_withDefaults_createsSuccessfully() {
        InteractionEntity saved = makeEntity();
        when(interactionRepository.save(any())).thenReturn(saved);

        InteractionCreateRequest req = new InteractionCreateRequest("João", null, "Aracaju", null, null, null);
        InteractionResponse response = interactionService.create(req);

        assertThat(response.contactName()).isEqualTo("João");
        verify(interactionRepository).save(any());
    }

    @Test
    void addMessage_inbound_setsStatusToAnswered() {
        InteractionEntity interaction = makeEntity();
        UUID interactionId = interaction.getId();
        when(interactionRepository.findById(interactionId)).thenReturn(Optional.of(interaction));

        InteractionMessageEntity savedMsg = new InteractionMessageEntity();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setInteraction(interaction);
        savedMsg.setDirection(MessageDirection.INBOUND);
        savedMsg.setBody("Olá");
        savedMsg.setSentAt(LocalDateTime.now());
        when(messageRepository.save(any())).thenReturn(savedMsg);
        when(interactionRepository.save(any())).thenReturn(interaction);

        InteractionMessageCreateRequest req = new InteractionMessageCreateRequest(MessageDirection.INBOUND, "Olá", null);
        InteractionMessageResponse response = interactionService.addMessage(interactionId, req);

        assertThat(response.direction()).isEqualTo(MessageDirection.INBOUND);
        assertThat(interaction.getStatus()).isEqualTo(InteractionStatus.ANSWERED);
    }

    @Test
    void delete_nonExistent_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(interactionRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> interactionService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
