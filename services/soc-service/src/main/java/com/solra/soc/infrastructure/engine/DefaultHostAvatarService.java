package com.solra.soc.infrastructure.engine;

import com.solra.soc.domain.model.HostAvatar;
import com.solra.soc.domain.repository.HostAvatarRepository;
import com.solra.soc.domain.service.HostAvatarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * DefaultHostAvatarService — 虚拟人主持人机制实现。
 * <p>
 * SOC-008: 自动协调多人互动节奏。
 * 冷场检测基于最后活动时间，超过阈值则触发引导建议。
 */
@Component
public class DefaultHostAvatarService implements HostAvatarService {

    private static final Logger log = LoggerFactory.getLogger(DefaultHostAvatarService.class);

    /** 冷场阈值：30秒无互动视为冷场 */
    private static final long SILENCE_THRESHOLD_SEC = 30;

    /** 最大发言时长：180秒 */
    private static final int MAX_SPEAKING_DURATION_SEC = 180;

    private final HostAvatarRepository hostRepository;

    /** 默认话题库 */
    private static final List<String> DEFAULT_TOPICS = List.of(
            "大家觉得这个空间怎么样？",
            "有没有人想分享最近的有趣经历？",
            "你们觉得索拉最吸引人的地方是什么？",
            "有没有推荐的其他空间？",
            "聊聊你们的虚拟人好友吧！",
            "大家平时喜欢在什么类型的空间里待着？"
    );

    /** 冷场建议动作 */
    private static final List<String> SILENCE_ACTIONS = List.of(
            "suggest_topic",        // 建议新话题
            "ice_breaker",          // 破冰游戏
            "poll",                 // 发起投票
            "compliment",           // 赞美空间
            "invite_speaker",       // 邀请某人发言
            "play_bgm"              // 播放背景音乐
    );

    public DefaultHostAvatarService(HostAvatarRepository hostRepository) {
        this.hostRepository = hostRepository;
    }

    @Override
    public HostAvatar createHost(String sessionId, String avatarId, String avatarName, String mode) {
        String hostId = UUID.randomUUID().toString();
        HostAvatar.HostMode hostMode = HostAvatar.HostMode.valueOf(mode.toUpperCase());
        HostAvatar host = new HostAvatar(hostId, sessionId, avatarId, avatarName, hostMode);

        // 预加载默认话题
        DEFAULT_TOPICS.forEach(host::addTopic);

        HostAvatar saved = hostRepository.save(host);
        log.info("Host avatar created: {} for session {} mode={}", hostId, sessionId, mode);
        return saved;
    }

    @Override
    public Optional<HostAvatar> getHost(String sessionId) {
        return hostRepository.findBySessionId(sessionId);
    }

    @Override
    public Optional<HostAvatar> getHostById(String hostId) {
        return hostRepository.findById(hostId);
    }

    @Override
    public List<HostAvatar> listSessionHosts(String sessionId) {
        return hostRepository.findAllBySessionId(sessionId);
    }

    @Override
    public HostAvatar startHosting(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        host.start();
        HostAvatar saved = hostRepository.save(host);
        log.info("Host {} started hosting", hostId);
        return saved;
    }

    @Override
    public void pauseHosting(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        host.pause();
        hostRepository.save(host);
        log.info("Host {} paused", hostId);
    }

    @Override
    public void resumeHosting(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        host.resume();
        hostRepository.save(host);
        log.info("Host {} resumed", hostId);
    }

    @Override
    public void stopHosting(String hostId) {
        hostRepository.deleteById(hostId);
        log.info("Host {} stopped", hostId);
    }

    @Override
    public HostAvatar switchMode(String hostId, String mode) {
        HostAvatar host = getOrThrow(hostId);
        HostAvatar.HostMode newMode = HostAvatar.HostMode.valueOf(mode.toUpperCase());
        host.switchMode(newMode);
        HostAvatar saved = hostRepository.save(host);
        log.info("Host {} switched mode to {}", hostId, mode);
        return saved;
    }

    @Override
    public HostAvatar setTopic(String hostId, String topic) {
        HostAvatar host = getOrThrow(hostId);
        host.setCurrentTopic(topic);
        HostAvatar saved = hostRepository.save(host);
        log.info("Host {} set topic: {}", hostId, topic);
        return saved;
    }

    @Override
    public void addTopicToQueue(String hostId, String topic) {
        HostAvatar host = getOrThrow(hostId);
        host.addTopic(topic);
        hostRepository.save(host);
    }

    @Override
    public String nextTopic(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        String next = host.nextTopic();
        hostRepository.save(host);
        log.info("Host {} advanced to next topic: {}", hostId, next);
        return next;
    }

    @Override
    public void addToSpeakerQueue(String hostId, String userId) {
        HostAvatar host = getOrThrow(hostId);
        host.addToSpeakerQueue(userId);
        hostRepository.save(host);
        log.debug("User {} added to speaker queue of host {}", userId, hostId);
    }

    @Override
    public void removeFromSpeakerQueue(String hostId, String userId) {
        HostAvatar host = getOrThrow(hostId);
        host.removeFromSpeakerQueue(userId);
        hostRepository.save(host);
    }

    @Override
    public String grantSpeakingTurn(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        String speaker = host.grantSpeakingTurn();
        hostRepository.save(host);
        if (speaker != null) {
            log.info("Host {} granted speaking turn to {}", hostId, speaker);
        }
        return speaker;
    }

    @Override
    public void endSpeakingTurn(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        host.endSpeakingTurn();
        hostRepository.save(host);
        log.info("Host {} ended speaking turn", hostId);
    }

    @Override
    public String detectSilence(String sessionId) {
        Optional<HostAvatar> hostOpt = hostRepository.findBySessionId(sessionId);
        if (hostOpt.isEmpty()) return null;

        HostAvatar host = hostOpt.get();
        if (!host.isActive()) return null;

        long silenceSec = Duration.between(host.getLastActivityAt(), Instant.now()).getSeconds();
        if (silenceSec < SILENCE_THRESHOLD_SEC) return null;

        log.info("Silence detected in session {} ({}s idle), host {} suggesting action",
                sessionId, silenceSec, host.getHostId());

        // 根据冷场时长选择建议
        String action;
        if (silenceSec < 60) {
            action = SILENCE_ACTIONS.get(new Random().nextInt(3)); // suggest_topic/ice_breaker/poll
        } else if (silenceSec < 120) {
            action = SILENCE_ACTIONS.get(2 + new Random().nextInt(3)); // poll/compliment/invite_speaker
        } else {
            action = "play_bgm"; // 长时间冷场放音乐
        }

        return action;
    }

    @Override
    public void recordInteraction(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        host.recordInteraction();
        hostRepository.save(host);
    }

    @Override
    public HostStats getHostStats(String hostId) {
        HostAvatar host = getOrThrow(hostId);
        long runningMinutes = Duration.between(host.getStartedAt(), Instant.now()).toMinutes();
        return new HostStats(
                host.getHostId(), host.getSessionId(),
                host.getMode().name(), host.getState().name(),
                host.getCurrentTopic(),
                host.getTopicQueueSize(), host.getSpeakerQueueSize(),
                host.getTotalInteractions(), runningMinutes);
    }

    private HostAvatar getOrThrow(String hostId) {
        return hostRepository.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException("Host not found: " + hostId));
    }
}
