package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.IncidentEntity;
import br.com.portal.decode_api.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    long countByStatus(IncidentStatus status);

    Optional<IncidentEntity> findTop1BySite_IdAndStatusOrderByOpenedAtDesc(UUID siteId, IncidentStatus status);

    List<IncidentEntity> findByStatusOrderByOpenedAtDesc(IncidentStatus status);

    @Query("""
            select i
            from IncidentEntity i
            where i.site.id = :siteId and i.openedAt >= :since
            order by i.openedAt desc
            """)
    List<IncidentEntity> listBySiteSince(@Param("siteId") UUID siteId, @Param("since") LocalDateTime since);
}
