package com.solra.avt.interfaces.grpc;

import com.solra.avt.application.dto.*;
import com.solra.avt.application.service.AvtApplicationService;
import com.solra.avt.domain.model.*;
import com.solra.apis.common.v1.Common;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;

/**
 * AvtGrpcService — AVT gRPC 接口适配层。
 * 注意：proto 生成的 Java 类型与 domain.model 命名冲突，
 * 使用完全限定名 com.solra.apis.avt.v1.* 引用 proto 类型。
 */
@GrpcService
public class AvtGrpcService extends com.solra.apis.avt.v1.AvtServiceGrpc.AvtServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AvtGrpcService.class);
    private final AvtApplicationService appService;

    public AvtGrpcService(AvtApplicationService appService) {
        this.appService = appService;
    }

    @Override
    public void sendMessage(com.solra.apis.avt.v1.SendMessageRequest request,
                             StreamObserver<com.solra.apis.avt.v1.SendMessageResponse> responseObserver) {
        try {
            SendMessageCommand cmd = new SendMessageCommand();
            cmd.setUserId(request.getUserId().getValue());
            cmd.setSpaceId(request.getSpaceId().getValue());
            cmd.setConversationId(request.getConversationId());
            cmd.setContent(request.getContent());
            cmd.setContext(request.getContextMap());

            List<MessageAttachment> attachments = new ArrayList<>();
            for (var att : request.getAttachmentsList()) {
                MessageAttachment ma = new MessageAttachment();
                ma.setAttachmentType(att.getAttachmentType());
                ma.setUrl(att.getUrl());
                ma.setMimeType(att.getMimeType());
                ma.setSizeBytes(att.getSizeBytes());
                attachments.add(ma);
            }
            cmd.setAttachments(attachments);

            AvtResultDTO.MessageSentResponse result = appService.sendMessage(cmd);

            var turnBuilder = com.solra.apis.avt.v1.DialogueTurn.newBuilder()
                    .setTurnId(result.turnId())
                    .setConversationId(result.conversationId())
                    .setRole(mapTurnRole(result.role()))
                    .setContent(nn(result.content()))
                    .setTimestamp(ts(result.timestamp()));

            for (var chunk : result.chunks()) {
                turnBuilder.addChunks(com.solra.apis.avt.v1.TokenChunk.newBuilder()
                        .setSequence(chunk.sequence()).setToken(chunk.token()).setIsFinal(chunk.isFinal()).build());
            }

            responseObserver.onNext(com.solra.apis.avt.v1.SendMessageResponse.newBuilder()
                    .setConversationId(result.conversationId()).setTurn(turnBuilder.build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("SendMessage failed", e);
            responseObserver.onNext(com.solra.apis.avt.v1.SendMessageResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder().setMessage(e.getMessage()).build()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void streamResponse(com.solra.apis.avt.v1.StreamResponseRequest request,
                                StreamObserver<com.solra.apis.avt.v1.DialogueTurn> responseObserver) {
        try {
            var engine = appService.getInferenceEngine();
            var publisher = engine.inferStream(request.getConversationId(), "", List.of(), Map.of());
            var turnId = UUID.randomUUID().toString();
            var convId = request.getConversationId();

            publisher.subscribe(new FlowSubscriberAdapter<>(
                    chunk -> responseObserver.onNext(
                            com.solra.apis.avt.v1.DialogueTurn.newBuilder()
                                    .setTurnId(turnId).setConversationId(convId)
                                    .setRole(com.solra.apis.avt.v1.TurnRole.TURN_ROLE_AVATAR)
                                    .setContent(chunk.getToken())
                                    .addChunks(com.solra.apis.avt.v1.TokenChunk.newBuilder()
                                            .setSequence(chunk.getSequence()).setToken(chunk.getToken())
                                            .setIsFinal(chunk.isFinal()).build()).build()),
                    responseObserver::onCompleted,
                    e -> { log.error("StreamResponse failed", e); responseObserver.onError(e); }));
        } catch (Exception e) {
            log.error("StreamResponse setup failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getConversationHistory(com.solra.apis.avt.v1.GetConversationHistoryRequest request,
                                        StreamObserver<com.solra.apis.avt.v1.GetConversationHistoryResponse> responseObserver) {
        try {
            int off = request.hasPage() ? (int) request.getPage().getPage() * (int) request.getPage().getSize() : 0;
            int lim = request.hasPage() ? (int) request.getPage().getSize() : 20;
            AvtResultDTO.ConversationHistoryDTO hist = appService.getHistory(request.getConversationId(), off, lim);

            List<com.solra.apis.avt.v1.DialogueTurn> protoTurns = new ArrayList<>();
            for (var d : hist.turns()) {
                protoTurns.add(com.solra.apis.avt.v1.DialogueTurn.newBuilder()
                        .setTurnId(d.turnId()).setConversationId(d.conversationId())
                        .setRole(mapTurnRoleStr(d.role())).setContent(nn(d.content()))
                        .setTimestamp(ts(d.timestamp())).build());
            }
            responseObserver.onNext(com.solra.apis.avt.v1.GetConversationHistoryResponse.newBuilder()
                    .addAllTurns(protoTurns)
                    .setPage(Common.PageResponse.newBuilder().setTotalItems(hist.total()).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetConversationHistory failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAvatarState(com.solra.apis.avt.v1.GetAvatarStateRequest request,
                                StreamObserver<com.solra.apis.avt.v1.GetAvatarStateResponse> responseObserver) {
        try {
            AvtResultDTO.AvatarStateDTO state = appService.getAvatarState(request.getAvatarId());
            responseObserver.onNext(com.solra.apis.avt.v1.GetAvatarStateResponse.newBuilder()
                    .setState(com.solra.apis.avt.v1.AvatarState.newBuilder()
                            .setAvatarId(state.avatarId())
                            .setEmotion(com.solra.apis.avt.v1.EmotionState.newBuilder()
                                    .setPrimaryEmotion(mapEmotion(state.primaryEmotion()))
                                    .setIntensity(state.emotionIntensity()).build())
                            .setActivity(mapActivity(state.activity()))
                            .setLastActive(ts(state.lastActive())).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetAvatarState failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateEmotionState(com.solra.apis.avt.v1.UpdateEmotionStateRequest request,
                                    StreamObserver<com.solra.apis.avt.v1.UpdateEmotionStateResponse> responseObserver) {
        try {
            var protoEm = request.getEmotion();
            AvtResultDTO.EmotionStateDTO result = appService.updateEmotionState(
                    request.getAvatarId(),
                    EmotionCategory.valueOf(protoEm.getPrimaryEmotion().name().replace("EMOTION_CATEGORY_", "")),
                    protoEm.getIntensity());
            responseObserver.onNext(com.solra.apis.avt.v1.UpdateEmotionStateResponse.newBuilder()
                    .setUpdatedEmotion(com.solra.apis.avt.v1.EmotionState.newBuilder()
                            .setPrimaryEmotion(mapEmotion(result.primaryEmotion()))
                            .setIntensity(result.intensity()).setDetectedAt(ts(result.detectedAt())).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("UpdateEmotionState failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void queryMemory(com.solra.apis.avt.v1.QueryMemoryRequest request,
                             StreamObserver<com.solra.apis.avt.v1.QueryMemoryResponse> responseObserver) {
        try {
            var q = request.getQuery();
            List<MemoryType> types = q.getMemoryTypesList().stream()
                    .map(t -> MemoryType.valueOf(t.name().replace("MEMORY_TYPE_", ""))).toList();
            int max = q.getMaxResults() > 0 ? q.getMaxResults() : 10;
            List<AvtResultDTO.MemoryDTO> memories = appService.queryMemory(q.getUserId().getValue(), types, q.getMinImportance(), max);

            List<com.solra.apis.avt.v1.MemoryEntry> protoMems = new ArrayList<>();
            for (var m : memories) {
                protoMems.add(com.solra.apis.avt.v1.MemoryEntry.newBuilder()
                        .setMemoryId(m.memoryId())
                        .setUserId(Common.UserId.newBuilder().setValue(m.userId()).build())
                        .setType(com.solra.apis.avt.v1.MemoryType.valueOf("MEMORY_TYPE_" + m.type()))
                        .setContent(m.content()).setImportance(m.importance())
                        .setCreatedAt(ts(m.createdAt())).setLastAccessed(ts(m.lastAccessed())).build());
            }
            responseObserver.onNext(com.solra.apis.avt.v1.QueryMemoryResponse.newBuilder()
                    .addAllMemories(protoMems).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("QueryMemory failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== AVT-003: Long-Term Memory ==========

    @Override
    public void getLongTermMemory(com.solra.apis.avt.v1.GetLongTermMemoryRequest request,
                                   StreamObserver<com.solra.apis.avt.v1.GetLongTermMemoryResponse> responseObserver) {
        try {
            var result = appService.getLongTermMemory(request.getUserId().getValue(), request.getAvatarId());

            var summaryBuilder = com.solra.apis.avt.v1.MemorySummaryDTO.newBuilder()
                    .addAllKnownFacts(result.summary().knownFacts())
                    .addAllPreferences(result.summary().preferences())
                    .addAllTopics(result.summary().topics())
                    .setRelationshipStage(result.summary().relationshipStage())
                    .setTotalInteractions(result.summary().totalInteractions());

            List<com.solra.apis.avt.v1.MemorySnapshotDTO> protoSnapshots = new ArrayList<>();
            for (var s : result.snapshots()) {
                protoSnapshots.add(com.solra.apis.avt.v1.MemorySnapshotDTO.newBuilder()
                        .setSnapshotId(s.snapshotId())
                        .setConversationId(s.conversationId())
                        .setContent(nn(s.content()))
                        .setType(s.type())
                        .setImportance(s.importance())
                        .setEmotionContext(nn(s.emotionContext()))
                        .setCapturedAt(ts(s.capturedAt())).build());
            }

            responseObserver.onNext(com.solra.apis.avt.v1.GetLongTermMemoryResponse.newBuilder()
                    .setSummary(summaryBuilder.build())
                    .addAllSnapshots(protoSnapshots).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetLongTermMemory failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void addMemorySnapshot(com.solra.apis.avt.v1.AddMemorySnapshotRequest request,
                                   StreamObserver<com.solra.apis.avt.v1.AddMemorySnapshotResponse> responseObserver) {
        try {
            appService.addMemorySnapshot(request.getUserId().getValue(), request.getAvatarId(),
                    request.getConversationId(), request.getContent(),
                    request.getSnapshotType(), request.getImportance(),
                    request.getEmotionContext());
            responseObserver.onNext(com.solra.apis.avt.v1.AddMemorySnapshotResponse.newBuilder()
                    .setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("AddMemorySnapshot failed", e);
            responseObserver.onNext(com.solra.apis.avt.v1.AddMemorySnapshotResponse.newBuilder()
                    .setSuccess(false)
                    .setError(Common.SolraError.newBuilder().setMessage(e.getMessage()).build()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void retrieveMemories(com.solra.apis.avt.v1.RetrieveMemoriesRequest request,
                                  StreamObserver<com.solra.apis.avt.v1.RetrieveMemoriesResponse> responseObserver) {
        try {
            int max = request.getMaxResults() > 0 ? request.getMaxResults() : 5;
            var results = appService.retrieveMemories(request.getUserId().getValue(),
                    request.getAvatarId(), request.getQuery(), max);

            List<com.solra.apis.avt.v1.MemorySnapshotDTO> protoSnapshots = new ArrayList<>();
            for (var s : results) {
                protoSnapshots.add(com.solra.apis.avt.v1.MemorySnapshotDTO.newBuilder()
                        .setSnapshotId(s.snapshotId())
                        .setConversationId(s.conversationId())
                        .setContent(nn(s.content()))
                        .setType(s.type())
                        .setImportance(s.importance())
                        .setEmotionContext(nn(s.emotionContext()))
                        .setCapturedAt(ts(s.capturedAt())).build());
            }

            responseObserver.onNext(com.solra.apis.avt.v1.RetrieveMemoriesResponse.newBuilder()
                    .addAllSnapshots(protoSnapshots).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("RetrieveMemories failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== AVT-004: 5D Emotion Model ==========

    @Override
    public void getEmotionState(com.solra.apis.avt.v1.GetEmotionStateRequest request,
                                 StreamObserver<com.solra.apis.avt.v1.GetEmotionStateResponse> responseObserver) {
        try {
            var em = appService.getEmotionState(request.getAvatarId());
            responseObserver.onNext(com.solra.apis.avt.v1.GetEmotionStateResponse.newBuilder()
                    .setEmotion(com.solra.apis.avt.v1.FiveDEmotionState.newBuilder()
                            .setJoy(em.joy()).setCuriosity(em.curiosity())
                            .setColdness(em.coldness()).setJealousy(em.jealousy())
                            .setSadness(em.sadness())
                            .setDominantDimension(nn(em.dominantDimension()))
                            .setCurrentMood(nn(em.currentMood()))
                            .setLastUpdated(ts(em.lastUpdated())).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetEmotionState failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void applyEmotionEvent(com.solra.apis.avt.v1.ApplyEmotionEventRequest request,
                                   StreamObserver<com.solra.apis.avt.v1.ApplyEmotionEventResponse> responseObserver) {
        try {
            var em = appService.applyEmotionEvent(request.getAvatarId(),
                    request.getEventType(), request.getIntensity());
            responseObserver.onNext(com.solra.apis.avt.v1.ApplyEmotionEventResponse.newBuilder()
                    .setEmotion(com.solra.apis.avt.v1.FiveDEmotionState.newBuilder()
                            .setJoy(em.joy()).setCuriosity(em.curiosity())
                            .setColdness(em.coldness()).setJealousy(em.jealousy())
                            .setSadness(em.sadness())
                            .setDominantDimension(nn(em.dominantDimension()))
                            .setCurrentMood(nn(em.currentMood()))
                            .setLastUpdated(ts(em.lastUpdated())).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("ApplyEmotionEvent failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void decayEmotions(com.solra.apis.avt.v1.DecayEmotionsRequest request,
                               StreamObserver<com.solra.apis.avt.v1.DecayEmotionsResponse> responseObserver) {
        try {
            var em = appService.decayEmotions(request.getAvatarId(), request.getRate());
            responseObserver.onNext(com.solra.apis.avt.v1.DecayEmotionsResponse.newBuilder()
                    .setEmotion(com.solra.apis.avt.v1.FiveDEmotionState.newBuilder()
                            .setJoy(em.joy()).setCuriosity(em.curiosity())
                            .setColdness(em.coldness()).setJealousy(em.jealousy())
                            .setSadness(em.sadness())
                            .setDominantDimension(nn(em.dominantDimension()))
                            .setCurrentMood(nn(em.currentMood()))
                            .setLastUpdated(ts(em.lastUpdated())).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("DecayEmotions failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== AVT-006: Avatar Expression ==========

    @Override
    public void getAvatarExpression(com.solra.apis.avt.v1.GetAvatarExpressionRequest request,
                                     StreamObserver<com.solra.apis.avt.v1.GetAvatarExpressionResponse> responseObserver) {
        try {
            var expr = appService.getAvatarExpression(request.getAvatarId(),
                    request.getExpressionType(), request.getIntensity());

            List<com.solra.apis.avt.v1.BlendshapeWeightDTO> protoBlendshapes = new ArrayList<>();
            for (var b : expr.blendshapes()) {
                protoBlendshapes.add(com.solra.apis.avt.v1.BlendshapeWeightDTO.newBuilder()
                        .setBlendshapeName(b.blendshapeName()).setWeight(b.weight()).build());
            }

            responseObserver.onNext(com.solra.apis.avt.v1.GetAvatarExpressionResponse.newBuilder()
                    .setExpression(com.solra.apis.avt.v1.AvatarExpressionDTO.newBuilder()
                            .setExpressionId(expr.expressionId())
                            .setExpressionType(expr.expressionType())
                            .setIntensity(expr.intensity())
                            .addAllBlendshapes(protoBlendshapes)
                            .setGesture(nn(expr.gesture()))
                            .setAnimationClip(nn(expr.animationClip())).build()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetAvatarExpression failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== AVT-008: Surprise Engine ==========

    @Override
    public void evaluateSurprise(com.solra.apis.avt.v1.EvaluateSurpriseRequest request,
                                  StreamObserver<com.solra.apis.avt.v1.EvaluateSurpriseResponse> responseObserver) {
        try {
            var moment = appService.evaluateSurprise(request.getUserId().getValue(),
                    request.getConversationId(), request.getAvatarId());

            var builder = com.solra.apis.avt.v1.EvaluateSurpriseResponse.newBuilder()
                    .setTriggered(moment != null);

            if (moment != null) {
                List<com.solra.apis.avt.v1.BlendshapeWeightDTO> protoBlendshapes = new ArrayList<>();
                for (var b : moment.expression().blendshapes()) {
                    protoBlendshapes.add(com.solra.apis.avt.v1.BlendshapeWeightDTO.newBuilder()
                            .setBlendshapeName(b.blendshapeName()).setWeight(b.weight()).build());
                }

                var exprDTO = com.solra.apis.avt.v1.AvatarExpressionDTO.newBuilder()
                        .setExpressionId(moment.expression().expressionId())
                        .setExpressionType(moment.expression().expressionType())
                        .setIntensity(moment.expression().intensity())
                        .addAllBlendshapes(protoBlendshapes)
                        .setGesture(nn(moment.expression().gesture()))
                        .setAnimationClip(nn(moment.expression().animationClip())).build();

                builder.setMoment(com.solra.apis.avt.v1.SurpriseMomentDTO.newBuilder()
                        .setSurpriseId(moment.surpriseId())
                        .setUserId(moment.userId())
                        .setConversationId(moment.conversationId())
                        .setSurpriseType(moment.surpriseType())
                        .setMessage(moment.message())
                        .setExpression(exprDTO)
                        .setGeneratedAt(ts(moment.generatedAt())).build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EvaluateSurprise failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSurpriseStats(com.solra.apis.avt.v1.GetSurpriseStatsRequest request,
                                  StreamObserver<com.solra.apis.avt.v1.GetSurpriseStatsResponse> responseObserver) {
        try {
            var stats = appService.getSurpriseStats(request.getUserId().getValue());
            responseObserver.onNext(com.solra.apis.avt.v1.GetSurpriseStatsResponse.newBuilder()
                    .setUserId(stats.userId())
                    .setTotalSurprises(stats.totalSurprises())
                    .setTodaySurprises(stats.todaySurprises()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetSurpriseStats failed", e);
            responseObserver.onError(e);
        }
    }

    // ---- mapping helpers ----
    private com.solra.apis.avt.v1.TurnRole mapTurnRole(TurnRole r) {
        return switch (r) {
            case USER -> com.solra.apis.avt.v1.TurnRole.TURN_ROLE_USER;
            case AVATAR -> com.solra.apis.avt.v1.TurnRole.TURN_ROLE_AVATAR;
            case SYSTEM -> com.solra.apis.avt.v1.TurnRole.TURN_ROLE_SYSTEM;
        };
    }
    private com.solra.apis.avt.v1.TurnRole mapTurnRoleStr(String r) {
        return com.solra.apis.avt.v1.TurnRole.valueOf("TURN_ROLE_" + r.toUpperCase());
    }
    private com.solra.apis.avt.v1.EmotionCategory mapEmotion(String n) {
        return com.solra.apis.avt.v1.EmotionCategory.valueOf("EMOTION_CATEGORY_" + n.toUpperCase());
    }
    private com.solra.apis.avt.v1.ActivityState mapActivity(String n) {
        return com.solra.apis.avt.v1.ActivityState.valueOf("ACTIVITY_STATE_" + n.toUpperCase());
    }
    private Common.SolraTimestamp ts(Instant i) {
        return i == null ? Common.SolraTimestamp.getDefaultInstance()
                : Common.SolraTimestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }
    private String nn(String s) { return s != null ? s : ""; }

    private static class FlowSubscriberAdapter<T> implements Flow.Subscriber<T> {
        private final java.util.function.Consumer<T> onNext;
        private final Runnable onComplete;
        private final java.util.function.Consumer<Throwable> onError;
        FlowSubscriberAdapter(java.util.function.Consumer<T> n, Runnable c, java.util.function.Consumer<Throwable> e) {
            this.onNext = n; this.onComplete = c; this.onError = e;
        }
        @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
        @Override public void onNext(T item) { onNext.accept(item); }
        @Override public void onError(Throwable t) { onError.accept(t); }
        @Override public void onComplete() { onComplete.run(); }
    }
}
