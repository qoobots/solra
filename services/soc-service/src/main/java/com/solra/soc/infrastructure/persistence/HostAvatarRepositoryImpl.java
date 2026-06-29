package com.solra.soc.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solra.soc.domain.model.HostAvatar;
import com.solra.soc.domain.repository.HostAvatarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HostAvatarRepositoryImpl — 虚拟人主持人仓储 JPA 实现。
 */
@Component
public class HostAvatarRepositoryImpl implements HostAvatarRepository {

    private static final Logger log = LoggerFactory.getLogger(HostAvatarRepositoryImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final HostAvatarJpaRepository jpaRepo;

    public HostAvatarRepositoryImpl(HostAvatarJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public HostAvatar save(HostAvatar host) {
        HostAvatarEntity entity = toEntity(host);
        HostAvatarEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<HostAvatar> findById(String hostId) {
        return jpaRepo.findById(hostId).map(this::toDomain);
    }

    @Override
    public Optional<HostAvatar> findBySessionId(String sessionId) {
        return jpaRepo.findBySessionId(sessionId).map(this::toDomain);
    }

    @Override
    public List<HostAvatar> findAllBySessionId(String sessionId) {
        return jpaRepo.findAllBySessionId(sessionId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String hostId) {
        jpaRepo.deleteById(hostId);
    }

    // ---- mapping ----

    HostAvatar toDomain(HostAvatarEntity e) {
        HostAvatar h = new HostAvatar();
        h.setHostId(e.getHostId());
        h.setSessionId(e.getSessionId());
        h.setAvatarId(e.getAvatarId());
        h.setAvatarName(e.getAvatarName());
        h.setMode(HostAvatar.HostMode.valueOf(e.getMode()));
        h.setState(HostAvatar.HostState.valueOf(e.getState()));
        h.setCurrentTopic(e.getCurrentTopic());
        h.setTopicQueue(parseJsonList(e.getTopicQueue()));
        h.setSpeakerQueue(parseJsonList(e.getSpeakerQueue()));
        h.setActiveSpeaker(e.getActiveSpeaker());
        h.setSpeakingDurationSec(e.getSpeakingDurationSec());
        h.setTotalInteractions(e.getTotalInteractions());
        h.setStartedAt(e.getStartedAt());
        h.setLastActivityAt(e.getLastActivityAt());
        return h;
    }

    HostAvatarEntity toEntity(HostAvatar h) {
        HostAvatarEntity e = new HostAvatarEntity();
        e.setHostId(h.getHostId());
        e.setSessionId(h.getSessionId());
        e.setAvatarId(h.getAvatarId());
        e.setAvatarName(h.getAvatarName());
        e.setMode(h.getMode().name());
        e.setState(h.getState().name());
        e.setCurrentTopic(h.getCurrentTopic());
        e.setTopicQueue(serializeJsonList(h.getTopicQueue()));
        e.setSpeakerQueue(serializeJsonList(h.getSpeakerQueue()));
        e.setActiveSpeaker(h.getActiveSpeaker());
        e.setSpeakingDurationSec(h.getSpeakingDurationSec());
        e.setTotalInteractions(h.getTotalInteractions());
        e.setStartedAt(h.getStartedAt());
        e.setLastActivityAt(h.getLastActivityAt());
        return e;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse JSON list", ex);
            return new ArrayList<>();
        }
    }

    private String serializeJsonList(List<String> list) {
        try {
            return mapper.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize JSON list", ex);
            return "[]";
        }
    }
}
