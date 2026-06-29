package com.solra.apis.saf.v1

import com.solra.apis.saf.v1.SafServiceGrpc.getServiceDescriptor
import io.grpc.CallOptions
import io.grpc.CallOptions.DEFAULT
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerServiceDefinition.builder
import io.grpc.ServiceDescriptor
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Holder for Kotlin coroutine-based client and server APIs for solra.saf.v1.SafService.
 */
public object SafServiceGrpcKt {
  public const val SERVICE_NAME: String = SafServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val submitReviewMethod: MethodDescriptor<Saf.SubmitReviewRequest, Saf.SubmitReviewResponse>
    @JvmStatic
    get() = SafServiceGrpc.getSubmitReviewMethod()

  public val queryReviewResultMethod:
      MethodDescriptor<Saf.QueryReviewResultRequest, Saf.QueryReviewResultResponse>
    @JvmStatic
    get() = SafServiceGrpc.getQueryReviewResultMethod()

  public val batchReviewMethod: MethodDescriptor<Saf.BatchReviewRequest, Saf.BatchReviewResponse>
    @JvmStatic
    get() = SafServiceGrpc.getBatchReviewMethod()

  public val getSafetyScoreMethod:
      MethodDescriptor<Saf.GetSafetyScoreRequest, Saf.GetSafetyScoreResponse>
    @JvmStatic
    get() = SafServiceGrpc.getGetSafetyScoreMethod()

  public val submitAppealMethod: MethodDescriptor<Saf.SubmitAppealRequest, Saf.SubmitAppealResponse>
    @JvmStatic
    get() = SafServiceGrpc.getSubmitAppealMethod()

  public val queryAppealResultMethod:
      MethodDescriptor<Saf.QueryAppealResultRequest, Saf.QueryAppealResultResponse>
    @JvmStatic
    get() = SafServiceGrpc.getQueryAppealResultMethod()

  public val filterContentMethod:
      MethodDescriptor<Saf.FilterContentRequest, Saf.FilterContentResponse>
    @JvmStatic
    get() = SafServiceGrpc.getFilterContentMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.saf.v1.SafService service as suspending coroutines.
   */
  @StubFor(SafServiceGrpc::class)
  public class SafServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<SafServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): SafServiceCoroutineStub = SafServiceCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun submitReview(request: Saf.SubmitReviewRequest, headers: Metadata = Metadata()): Saf.SubmitReviewResponse = unaryRpc(
      channel,
      SafServiceGrpc.getSubmitReviewMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun queryReviewResult(request: Saf.QueryReviewResultRequest, headers: Metadata = Metadata()): Saf.QueryReviewResultResponse = unaryRpc(
      channel,
      SafServiceGrpc.getQueryReviewResultMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun batchReview(request: Saf.BatchReviewRequest, headers: Metadata = Metadata()): Saf.BatchReviewResponse = unaryRpc(
      channel,
      SafServiceGrpc.getBatchReviewMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getSafetyScore(request: Saf.GetSafetyScoreRequest, headers: Metadata = Metadata()): Saf.GetSafetyScoreResponse = unaryRpc(
      channel,
      SafServiceGrpc.getGetSafetyScoreMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun submitAppeal(request: Saf.SubmitAppealRequest, headers: Metadata = Metadata()): Saf.SubmitAppealResponse = unaryRpc(
      channel,
      SafServiceGrpc.getSubmitAppealMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun queryAppealResult(request: Saf.QueryAppealResultRequest, headers: Metadata = Metadata()): Saf.QueryAppealResultResponse = unaryRpc(
      channel,
      SafServiceGrpc.getQueryAppealResultMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun filterContent(request: Saf.FilterContentRequest, headers: Metadata = Metadata()): Saf.FilterContentResponse = unaryRpc(
      channel,
      SafServiceGrpc.getFilterContentMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.saf.v1.SafService service based on Kotlin coroutines.
   */
  public abstract class SafServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.SubmitReview.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun submitReview(request: Saf.SubmitReviewRequest): Saf.SubmitReviewResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.SubmitReview is unimplemented"))

    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.QueryReviewResult.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun queryReviewResult(request: Saf.QueryReviewResultRequest): Saf.QueryReviewResultResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.QueryReviewResult is unimplemented"))

    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.BatchReview.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun batchReview(request: Saf.BatchReviewRequest): Saf.BatchReviewResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.BatchReview is unimplemented"))

    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.GetSafetyScore.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getSafetyScore(request: Saf.GetSafetyScoreRequest): Saf.GetSafetyScoreResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.GetSafetyScore is unimplemented"))

    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.SubmitAppeal.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun submitAppeal(request: Saf.SubmitAppealRequest): Saf.SubmitAppealResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.SubmitAppeal is unimplemented"))

    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.QueryAppealResult.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun queryAppealResult(request: Saf.QueryAppealResultRequest): Saf.QueryAppealResultResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.QueryAppealResult is unimplemented"))

    /**
     * Returns the response to an RPC for solra.saf.v1.SafService.FilterContent.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun filterContent(request: Saf.FilterContentRequest): Saf.FilterContentResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.saf.v1.SafService.FilterContent is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getSubmitReviewMethod(),
      implementation = ::submitReview
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getQueryReviewResultMethod(),
      implementation = ::queryReviewResult
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getBatchReviewMethod(),
      implementation = ::batchReview
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getGetSafetyScoreMethod(),
      implementation = ::getSafetyScore
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getSubmitAppealMethod(),
      implementation = ::submitAppeal
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getQueryAppealResultMethod(),
      implementation = ::queryAppealResult
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SafServiceGrpc.getFilterContentMethod(),
      implementation = ::filterContent
    )).build()
  }
}
