package com.solra.avt.infrastructure.persistence;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DialogueTurnRepositoryImpl implements DialogueTurnRepository {

    private final DialogueTurnJpaRepository jpa;

    public DialogueTurnRepositoryImpl(DialogueTurnJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<DialogueTurn> findByConversationId(String conversationId, int offset, int limit) {
        int page = offset / Math.max(1, limit);
        return jpa.findPageByConversationId(conversationId, PageRequest.of(page, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void save(DialogueTurn turn) {
        jpa.save(toEntity(turn));
    }

    @Override
    public void saveAll(List<DialogueTurn> turns) {
        jpa.saveAll(turns.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    @Override
    public long countByConversationId(String conversationId) {
        return jpa.countByConversationId(conversationId);
    }

    private DialogueTurn toDomain(DialogueTurnEntity e) {
        DialogueTurn t = new DialogueTurn();
        t.setTurnId(e.getTurnId());
        t.setConversationId(e.getConversationId());
        t.setRole(TurnRole.valueOf(e.getRole()));
        t.setContent(e.getContent());
        t.setTimestamp(e.getTimestamp());
        if (e.getChunks() != null) {
            t.setChunks(e.getChunks().stream().map(ch -> {
                TokenChunk tc = new TokenChunk();
                tc.setSequence(ch.getSequence());
                tc.setToken(ch.getToken());
                tc.setFinal(ch.isFinal());
                return tc;
            }).collect(Collectors.toList()));
        }
        t.setMetadata(e.getMetadata());
        return t;
    }

    private DialogueTurnEntity toEntity(DialogueTurn t) {
        DialogueTurnEntity e = new DialogueTurnEntity();
        e.setTurnId(t.getTurnId());
        e.setConversationId(t.getConversationId());
        e.setRole(t.getRole().name());
        e.setContent(t.getContent());
        e.setTimestamp(t.getTimestamp());
        if (t.getChunks() != null) {
            e.setChunks(t.getChunks().stream().map(ch -> {
                DialogueTurnEntity.TokenChunkEmbeddable emb = new DialogueTurnEntity.TokenChunkEmbeddable();
                emb.setSequence(ch.getSequence());
                emb.setToken(ch.getToken());
                emb.setFinal(ch.isFinal());
                return emb;
            }).collect(Collectors.toList()));
        }
        e.setMetadata(t.getMetadata());
        return e;
    }
}
