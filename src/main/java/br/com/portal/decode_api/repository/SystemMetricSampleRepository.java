package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.SystemMetricSampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemMetricSampleRepository extends JpaRepository<SystemMetricSampleEntity, UUID> {

    Optional<SystemMetricSampleEntity> findTop1ByOrderBySampledAtDesc();

    List<SystemMetricSampleEntity> findBySampledAtGreaterThanEqualOrderBySampledAtAsc(LocalDateTime since);
}