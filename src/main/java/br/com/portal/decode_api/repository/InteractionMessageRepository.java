package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.InteractionMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InteractionMessageRepository extends JpaRepository<InteractionMessageEntity, UUID> {

    @Query("""
            select m
            from InteractionMessageEntity m
            where m.interaction.id = :interactionId
            order by m.sentAt desc
            """)
    List<InteractionMessageEntity> findLatestByInteraction(@Param("interactionId") UUID interactionId);
}
