package br.com.portal.decode_api.repository;

import br.com.portal.decode_api.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    Optional<UserEntity> findByEmailIgnoreCaseAndActiveTrue(String email);

    @Query("""
            select u
            from UserEntity u
            where (:q is null or :q = ''
                or lower(u.name) like lower(concat('%', :q, '%'))
                or lower(u.email) like lower(concat('%', :q, '%'))
            )
            order by u.updatedAt desc
            """)
    Page<UserEntity> search(@Param("q") String q, Pageable pageable);
}
