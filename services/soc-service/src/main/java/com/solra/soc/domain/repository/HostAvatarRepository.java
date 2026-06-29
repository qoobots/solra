package com.solra.soc.domain.repository;

import com.solra.soc.domain.model.HostAvatar;

import java.util.List;
import java.util.Optional;

/**
 * HostAvatar 仓储接口。
 */
public interface HostAvatarRepository {

    HostAvatar save(HostAvatar host);

    Optional<HostAvatar> findById(String hostId);

    Optional<HostAvatar> findBySessionId(String sessionId);

    List<HostAvatar> findAllBySessionId(String sessionId);

    void deleteById(String hostId);
}
