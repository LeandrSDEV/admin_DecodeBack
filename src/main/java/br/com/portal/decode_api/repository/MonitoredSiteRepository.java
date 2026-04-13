package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.MonitoredSiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MonitoredSiteRepository extends JpaRepository<MonitoredSiteEntity, UUID> {

    @Query("""
            select s
            from MonitoredSiteEntity s
            where (:q is null or :q = ''
                or lower(s.code) like lower(concat(concat('%', :q), '%'))
                or lower(s.name) like lower(concat(concat('%', :q), '%'))
                or lower(s.url) like lower(concat(concat('%', :q), '%'))
            )
            order by s.updatedAt desc
            """)
    List<MonitoredSiteEntity> search(@Param("q") String q);

    List<MonitoredSiteEntity> findByEnabledTrue();
}
