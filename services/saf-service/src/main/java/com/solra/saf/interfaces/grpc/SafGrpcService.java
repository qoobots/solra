package com.solra.saf.interfaces.grpc;

import com.solra.saf.application.dto.FilterRequest;
import com.solra.saf.application.dto.FilterResultDTO;
import com.solra.saf.application.service.SafetyApplicationService;
import com.solra.saf.domain.model.*;
import com.solra.common.exception.SolraException;
import com.solra.apis.saf.v1.*;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * gRPC service implementation for content safety.
 * Implements all 7 RPCs defined in saf.proto.
 * P0 focus: FilterContent (SAF-002), SubmitReview/QueryReviewResult (SAF-001).
 */
@GrpcService
public class SafGrpcService {

    private static final Logger log = LoggerFactory.getLogger(SafGrpcService.class);
    private final SafetyApplicationService safetyAppService;

    public SafGrpcService(SafetyApplicationService safetyAppService) {
        this.safetyAppService = safetyAppService;
    }

    /**
     * SAF-002: Real-time content filtering — primary API for dialogue safety.
     * Called by AVT service before delivering avatar speech to user.
     */
    public void filterContent(FilterContentRequest request,
                               io.grpc.stub.StreamObserver<FilterContentResponse> responseObserver) {
        try {
            ContentType ct = mapContentType(request.getContentType());
            FilterRequest fr = new FilterRequest(
                    request.getUserId().getValue(), ct,
                    request.getContent(), request.getContentUrl(),
                    request.getFilterLevel().name()
            );

            FilterResultDTO result = safetyAppService.filterContent(fr);

            FilterContentResponse response = FilterContentResponse.newBuilder()
                    .setPassed(result.passed())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.debug("Content filtered: passed={}, type={}", result.passed(), ct);
        } catch (Exception e) {
            log.error("FilterContent failed", e);
            responseObserver.onNext(FilterContentResponse.newBuilder()
                    .setPassed(false)
                    .setError(toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * SAF-001: Submit content for review.
     */
    public void submitReview(SubmitReviewRequest request,
                              io.grpc.stub.StreamObserver<SubmitReviewResponse> responseObserver) {
        try {
            ContentTarget target = ContentTarget.text(
                    request.getTarget().getContentId(),
                    request.getTarget().getContentText()
            );

            ReviewCase rc = safetyAppService.submitForReview(
                    "system", target,
                    ReviewType.valueOf(request.getReviewType().name()),
                    ReviewPriority.valueOf(request.getPriority().name())
            );

            SubmitReviewResponse response = SubmitReviewResponse.newBuilder()
                    .setCaseId(rc.getCaseId())
                    .setStatus(ReviewStatus.valueOf(rc.getStatus().name()))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Review submitted: caseId={}, decision={}", rc.getCaseId(), rc.getDecision());
        } catch (Exception e) {
            responseObserver.onNext(SubmitReviewResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            responseObserver.onCompleted();
        }
    }

    /**
     * SAF-001: Query review result.
     */
    public void queryReviewResult(QueryReviewResultRequest request,
                                   io.grpc.stub.StreamObserver<QueryReviewResultResponse> responseObserver) {
        try {
            ReviewCase rc = safetyAppService.queryReviewCase(request.getCaseId());

            QueryReviewResultResponse response = QueryReviewResultResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onNext(QueryReviewResultResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            responseObserver.onCompleted();
        }
    }

    // -- Stubs for P1/P2 features --
    public void batchReview(BatchReviewRequest request,
                             io.grpc.stub.StreamObserver<BatchReviewResponse> observer) {
        observer.onNext(BatchReviewResponse.newBuilder().build());
        observer.onCompleted();
    }

    public void getSafetyScore(GetSafetyScoreRequest request,
                                io.grpc.stub.StreamObserver<GetSafetyScoreResponse> observer) {
        try {
            var score = safetyAppService.checkSafety(
                    request.getTarget().getContentText(),
                    mapContentType(request.getTarget().getContentType()));
            observer.onNext(GetSafetyScoreResponse.newBuilder().build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onNext(GetSafetyScoreResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

    public void submitAppeal(SubmitAppealRequest request,
                              io.grpc.stub.StreamObserver<SubmitAppealResponse> observer) {
        observer.onNext(SubmitAppealResponse.newBuilder()
                .setAppealId("appeal_" + UUID.randomUUID())
                .setStatus(AppealStatus.APPEAL_STATUS_SUBMITTED)
                .build());
        observer.onCompleted();
    }

    public void queryAppealResult(QueryAppealResultRequest request,
                                   io.grpc.stub.StreamObserver<QueryAppealResultResponse> observer) {
        observer.onNext(QueryAppealResultResponse.newBuilder()
                .setError(toProtoError(new SolraException.ServiceUnavailableException("Appeal coming in H2")))
                .build());
        observer.onCompleted();
    }

    // -- Helpers --

    private ContentType mapContentType(com.solra.apis.saf.v1.ContentType protoType) {
        return switch (protoType) {
            case CONTENT_TYPE_TEXT -> ContentType.TEXT;
            case CONTENT_TYPE_AVATAR_SPEECH -> ContentType.AVATAR_SPEECH;
            case CONTENT_TYPE_SPACE_NAME -> ContentType.SPACE_NAME;
            case CONTENT_TYPE_SPACE_DESCRIPTION -> ContentType.SPACE_DESCRIPTION;
            case CONTENT_TYPE_USER_PROFILE -> ContentType.USER_PROFILE;
            default -> ContentType.TEXT;
        };
    }

    private com.solra.apis.common.v1.SolraError toProtoError(Exception e) {
        return com.solra.apis.common.v1.SolraError.newBuilder()
                .setCode(com.solra.apis.common.v1.ErrorCode.ERROR_INTERNAL)
                .setMessage(e.getMessage())
                .build();
    }
}
