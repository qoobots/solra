package com.solra.not.domain.repository;

import com.solra.not.domain.model.PushMessage;
import java.util.Optional;

/** 推送消息仓储接口 */
public interface PushMessageRepository {
    PushMessage save(PushMessage push);
    Optional<PushMessage> findById(String pushId);
    void updateStatus(String pushId, String status, String failureReason);
}
