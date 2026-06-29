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

    // -- P1 Features: SAF-003, SAF-004, SAF-005 --

    /**
     * SAF-003: Submit a user report.
     */
    public void submitReport(SubmitReportRequest request,
                              io.grpc.stub.StreamObserver<SubmitReportResponse> observer) {
        try {
            com.solra.saf.domain.model.ReportCategory category = mapReportCategory(request.getCategory());
            var report = safetyAppService.submitReport(
                    request.getReporterUserId().getValue(),
                    request.getReportedUserId().getValue(),
                    request.getReportedContentId(),
                    request.getReason(),
                    category,
                    request.getEvidenceUrlsList());

            observer.onNext(SubmitReportResponse.newBuilder()
                    .setReportId(report.getReportId())
                    .setStatus(ReportStatus.valueOf(report.getStatus().name()))
                    .build());
            observer.onCompleted();
            log.info("Report submitted: reportId={}, category={}", report.getReportId(), category);
        } catch (Exception e) {
            log.error("SubmitReport failed", e);
            observer.onNext(SubmitReportResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

    /**
     * SAF-003: Query a report result.
     */
    public void queryReportResult(QueryReportResultRequest request,
                                   io.grpc.stub.StreamObserver<QueryReportResultResponse> observer) {
        try {
            var report = safetyAppService.queryReport(request.getReportId());
            observer.onNext(QueryReportResultResponse.newBuilder()
                    .setReportId(report.getReportId())
                    .setStatus(ReportStatus.valueOf(report.getStatus().name()))
                    .setDecision(report.getDecision() != null
                            ? ReviewDecision.valueOf(report.getDecision().name())
                            : ReviewDecision.REVIEW_DECISION_UNSPECIFIED)
                    .setDecisionReason(report.getDecisionReason() != null ? report.getDecisionReason() : "")
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onNext(QueryReportResultResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

    /**
     * SAF-004: Get manual review workbench queue.
     */
    public void getWorkQueue(GetWorkQueueRequest request,
                              io.grpc.stub.StreamObserver<GetWorkQueueResponse> observer) {
        try {
            var items = safetyAppService.getWorkQueue(request.getReviewerId(), request.getLimit());
            var responseBuilder = GetWorkQueueResponse.newBuilder();
            for (var item : items) {
                responseBuilder.addItems(com.solra.apis.saf.v1.ReviewWorkbenchItem.newBuilder()
                        .setItemId(item.getItemId())
                        .setCaseId(item.getReviewCaseId())
                        .setContentPreview(item.getTargetContentText() != null
                                ? item.getTargetContentText().substring(0,
                                        Math.min(200, item.getTargetContentText().length()))
                                : "")
                        .setContentType(mapProtoContentType(item.getContentType()))
                        .setPriority(ReviewPriority.valueOf(item.getPriority().name()))
                        .setQueuedAt(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(item.getQueuedAt().getEpochSecond()).build())
                        .build());
            }
            observer.onNext(responseBuilder.build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onNext(GetWorkQueueResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

    /**
     * SAF-004: Claim a review item.
     */
    public void claimReviewItem(ClaimReviewItemRequest request,
                                 io.grpc.stub.StreamObserver<ClaimReviewItemResponse> observer) {
        try {
            var item = safetyAppService.claimReviewItem(
                    request.getCaseId(), request.getReviewerId());
            observer.onNext(ClaimReviewItemResponse.newBuilder()
                    .setItemId(item.getItemId())
                    .setClaimed(true)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onNext(ClaimReviewItemResponse.newBuilder()
                    .setClaimed(false)
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

    /**
     * SAF-004: Submit manual review decision.
     */
    public void submitManualReview(SubmitManualReviewRequest request,
                                    io.grpc.stub.StreamObserver<SubmitManualReviewResponse> observer) {
        try {
            com.solra.saf.domain.model.ReviewDecision decision = mapReviewDecision(request.getDecision());
            var reviewCase = safetyAppService.submitManualReview(
                    request.getCaseId(), decision,
                    request.getReason(), request.getReviewerId());

            observer.onNext(SubmitManualReviewResponse.newBuilder()
                    .setCaseId(reviewCase.getCaseId())
                    .setStatus(ReviewStatus.valueOf(reviewCase.getStatus().name()))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onNext(SubmitManualReviewResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

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

    /**
     * SAF-005: Submit an appeal.
     */
    public void submitAppeal(SubmitAppealRequest request,
                              io.grpc.stub.StreamObserver<SubmitAppealResponse> observer) {
        try {
            var appeal = safetyAppService.submitAppeal(
                    request.getCaseId(),
                    request.getUserId().getValue(),
                    request.getReason(),
                    request.getEvidenceUrlsList());

            observer.onNext(SubmitAppealResponse.newBuilder()
                    .setAppealId(appeal.getAppealId())
                    .setStatus(AppealStatus.valueOf(appeal.getStatus().name()))
                    .build());
            observer.onCompleted();
            log.info("Appeal submitted: appealId={}, caseId={}", appeal.getAppealId(), request.getCaseId());
        } catch (Exception e) {
            log.error("SubmitAppeal failed", e);
            observer.onNext(SubmitAppealResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
    }

    /**
     * SAF-005: Query appeal result.
     */
    public void queryAppealResult(QueryAppealResultRequest request,
                                   io.grpc.stub.StreamObserver<QueryAppealResultResponse> observer) {
        try {
            var appeal = safetyAppService.queryAppeal(request.getAppealId());

            var protoAppeal = com.solra.apis.saf.v1.Appeal.newBuilder()
                    .setAppealId(appeal.getAppealId())
                    .setCaseId(appeal.getCaseId())
                    .setUserId(com.solra.apis.common.v1.UserId.newBuilder()
                            .setValue(appeal.getUserId()).build())
                    .setReason(appeal.getReason())
                    .setStatus(AppealStatus.valueOf(appeal.getStatus().name()))
                    .build();

            observer.onNext(QueryAppealResultResponse.newBuilder()
                    .setAppeal(protoAppeal)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onNext(QueryAppealResultResponse.newBuilder()
                    .setError(toProtoError(e)).build());
            observer.onCompleted();
        }
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

    private com.solra.apis.saf.v1.ContentType mapProtoContentType(ContentType domainType) {
        return switch (domainType) {
            case TEXT -> com.solra.apis.saf.v1.ContentType.CONTENT_TYPE_TEXT;
            case AVATAR_SPEECH -> com.solra.apis.saf.v1.ContentType.CONTENT_TYPE_AVATAR_SPEECH;
            case SPACE_NAME -> com.solra.apis.saf.v1.ContentType.CONTENT_TYPE_SPACE_NAME;
            case SPACE_DESCRIPTION -> com.solra.apis.saf.v1.ContentType.CONTENT_TYPE_SPACE_DESCRIPTION;
            case USER_PROFILE -> com.solra.apis.saf.v1.ContentType.CONTENT_TYPE_USER_PROFILE;
            default -> com.solra.apis.saf.v1.ContentType.CONTENT_TYPE_TEXT;
        };
    }

    private com.solra.saf.domain.model.ReportCategory mapReportCategory(
            com.solra.apis.saf.v1.ReportCategory protoCategory) {
        return switch (protoCategory) {
            case REPORT_CATEGORY_ILLEGAL -> com.solra.saf.domain.model.ReportCategory.ILLEGAL_CONTENT;
            case REPORT_CATEGORY_NSFW -> com.solra.saf.domain.model.ReportCategory.NSFW;
            case REPORT_CATEGORY_HATE_SPEECH -> com.solra.saf.domain.model.ReportCategory.HATE_SPEECH;
            case REPORT_CATEGORY_HARASSMENT -> com.solra.saf.domain.model.ReportCategory.HARASSMENT;
            case REPORT_CATEGORY_SPAM -> com.solra.saf.domain.model.ReportCategory.SPAM;
            case REPORT_CATEGORY_COPYRIGHT -> com.solra.saf.domain.model.ReportCategory.COPYRIGHT;
            case REPORT_CATEGORY_MINOR_SAFETY -> com.solra.saf.domain.model.ReportCategory.MINOR_SAFETY;
            default -> com.solra.saf.domain.model.ReportCategory.OTHER;
        };
    }

    private com.solra.saf.domain.model.ReviewDecision mapReviewDecision(
            com.solra.apis.saf.v1.ReviewDecision protoDecision) {
        return switch (protoDecision) {
            case REVIEW_DECISION_APPROVED -> com.solra.saf.domain.model.ReviewDecision.APPROVED;
            case REVIEW_DECISION_REJECTED -> com.solra.saf.domain.model.ReviewDecision.REJECTED;
            case REVIEW_DECISION_FLAGGED -> com.solra.saf.domain.model.ReviewDecision.FLAGGED;
            case REVIEW_DECISION_BLOCKED -> com.solra.saf.domain.model.ReviewDecision.BLOCKED;
            default -> com.solra.saf.domain.model.ReviewDecision.FLAGGED;
        };
    }

    private com.solra.apis.common.v1.SolraError toProtoError(Exception e) {
        return com.solra.apis.common.v1.SolraError.newBuilder()
                .setCode(com.solra.apis.common.v1.ErrorCode.ERROR_INTERNAL)
                .setMessage(e.getMessage())
                .build();
    }
}
