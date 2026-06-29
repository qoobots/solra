package com.solra.soc.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * HostAvatar JPA Repository。
 */
@Repository
public interface HostAvatarJpaRepository extends JpaRepository<HostAvatarEntity, String> {

    Optional<HostAvatarEntity> findBySessionId(String sessionId);

    List<HostAvatarEntity> findAllBySessionId(String sessionId);
}
