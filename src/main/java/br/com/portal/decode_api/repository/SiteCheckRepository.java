package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.SiteCheckEntity;
import br.com.portal.decode_api.enums.SiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteCheckRepository extends JpaRepository<SiteCheckEntity, UUID> {

    Optional<SiteCheckEntity> findTop1BySite_IdOrderByCheckedAtDesc(UUID siteId);

    @Query("""
            select count(c)
            from SiteCheckEntity c
            where c.site.id = :siteId and c.checkedAt >= :since
            """)
    long countSince(@Param("siteId") UUID siteId, @Param("since") LocalDateTime since);

    @Query("""
            select count(c)
            from SiteCheckEntity c
            where c.site.id = :siteId and c.checkedAt >= :since and c.status <> :down
            """)
    long countUpSince(@Param("siteId") UUID siteId, @Param("since") LocalDateTime since, @Param("down") SiteStatus down);

    @Query("""
            select c
            from SiteCheckEntity c
            where c.site.id = :siteId and c.checkedAt >= :since
            order by c.checkedAt asc
            """)
    List<SiteCheckEntity> listBySiteSince(@Param("siteId") UUID siteId, @Param("since") LocalDateTime since);

    @Query("""
            select c
            from SiteCheckEntity c
            where c.checkedAt >= :since
            order by c.checkedAt desc
            """)
    List<SiteCheckEntity> listRecent(@Param("since") LocalDateTime since);
}
