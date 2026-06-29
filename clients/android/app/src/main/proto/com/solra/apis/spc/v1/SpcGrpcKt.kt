package com.solra.apis.spc.v1

import com.solra.apis.spc.v1.SpcServiceGrpc.getServiceDescriptor
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
import io.grpc.kotlin.ClientCalls.serverStreamingRpc
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls.serverStreamingServerMethodDefinition
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

/**
 * Holder for Kotlin coroutine-based client and server APIs for solra.spc.v1.SpcService.
 */
public object SpcServiceGrpcKt {
  public const val SERVICE_NAME: String = SpcServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val getSpaceMethod: MethodDescriptor<Spc.GetSpaceRequest, Spc.GetSpaceResponse>
    @JvmStatic
    get() = SpcServiceGrpc.getGetSpaceMethod()

  public val listRecommendSpacesMethod:
      MethodDescriptor<Spc.ListRecommendSpacesRequest, Spc.ListRecommendSpacesResponse>
    @JvmStatic
    get() = SpcServiceGrpc.getListRecommendSpacesMethod()

  public val loadSpaceMethod: MethodDescriptor<Spc.LoadSpaceRequest, Spc.LoadSpaceResponse>
    @JvmStatic
    get() = SpcServiceGrpc.getLoadSpaceMethod()

  public val streamSpaceAssetsMethod: MethodDescriptor<Spc.StreamSpaceAssetsRequest, Spc.AssetChunk>
    @JvmStatic
    get() = SpcServiceGrpc.getStreamSpaceAssetsMethod()

  public val getPreviewCardMethod:
      MethodDescriptor<Spc.GetPreviewCardRequest, Spc.GetPreviewCardResponse>
    @JvmStatic
    get() = SpcServiceGrpc.getGetPreviewCardMethod()

  public val batchGetPreviewCardsMethod:
      MethodDescriptor<Spc.BatchGetPreviewCardsRequest, Spc.BatchGetPreviewCardsResponse>
    @JvmStatic
    get() = SpcServiceGrpc.getBatchGetPreviewCardsMethod()

  public val reportUserActionMethod:
      MethodDescriptor<Spc.ReportUserActionRequest, Spc.ReportUserActionResponse>
    @JvmStatic
    get() = SpcServiceGrpc.getReportUserActionMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.spc.v1.SpcService service as suspending coroutines.
   */
  @StubFor(SpcServiceGrpc::class)
  public class SpcServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<SpcServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): SpcServiceCoroutineStub = SpcServiceCoroutineStub(channel, callOptions)

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
    public suspend fun getSpace(request: Spc.GetSpaceRequest, headers: Metadata = Metadata()): Spc.GetSpaceResponse = unaryRpc(
      channel,
      SpcServiceGrpc.getGetSpaceMethod(),
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
    public suspend fun listRecommendSpaces(request: Spc.ListRecommendSpacesRequest, headers: Metadata = Metadata()): Spc.ListRecommendSpacesResponse = unaryRpc(
      channel,
      SpcServiceGrpc.getListRecommendSpacesMethod(),
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
    public suspend fun loadSpace(request: Spc.LoadSpaceRequest, headers: Metadata = Metadata()): Spc.LoadSpaceResponse = unaryRpc(
      channel,
      SpcServiceGrpc.getLoadSpaceMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Returns a [Flow] that, when collected, executes this RPC and emits responses from the
     * server as they arrive.  That flow finishes normally if the server closes its response with
     * [`Status.OK`][io.grpc.Status], and fails by throwing a [StatusException] otherwise.  If
     * collecting the flow downstream fails exceptionally (including via cancellation), the RPC
     * is cancelled with that exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return A flow that, when collected, emits the responses from the server.
     */
    public fun streamSpaceAssets(request: Spc.StreamSpaceAssetsRequest, headers: Metadata = Metadata()): Flow<Spc.AssetChunk> = serverStreamingRpc(
      channel,
      SpcServiceGrpc.getStreamSpaceAssetsMethod(),
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
    public suspend fun getPreviewCard(request: Spc.GetPreviewCardRequest, headers: Metadata = Metadata()): Spc.GetPreviewCardResponse = unaryRpc(
      channel,
      SpcServiceGrpc.getGetPreviewCardMethod(),
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
    public suspend fun batchGetPreviewCards(request: Spc.BatchGetPreviewCardsRequest, headers: Metadata = Metadata()): Spc.BatchGetPreviewCardsResponse = unaryRpc(
      channel,
      SpcServiceGrpc.getBatchGetPreviewCardsMethod(),
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
    public suspend fun reportUserAction(request: Spc.ReportUserActionRequest, headers: Metadata = Metadata()): Spc.ReportUserActionResponse = unaryRpc(
      channel,
      SpcServiceGrpc.getReportUserActionMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.spc.v1.SpcService service based on Kotlin coroutines.
   */
  public abstract class SpcServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.spc.v1.SpcService.GetSpace.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getSpace(request: Spc.GetSpaceRequest): Spc.GetSpaceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.GetSpace is unimplemented"))

    /**
     * Returns the response to an RPC for solra.spc.v1.SpcService.ListRecommendSpaces.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listRecommendSpaces(request: Spc.ListRecommendSpacesRequest): Spc.ListRecommendSpacesResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.ListRecommendSpaces is unimplemented"))

    /**
     * Returns the response to an RPC for solra.spc.v1.SpcService.LoadSpace.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun loadSpace(request: Spc.LoadSpaceRequest): Spc.LoadSpaceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.LoadSpace is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for solra.spc.v1.SpcService.StreamSpaceAssets.
     *
     * If creating or collecting the returned flow fails with a [StatusException], the RPC
     * will fail with the corresponding [io.grpc.Status].  If it fails with a
     * [java.util.concurrent.CancellationException], the RPC will fail with status `Status.CANCELLED`.  If creating
     * or collecting the returned flow fails for any other reason, the RPC will fail with
     * `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open fun streamSpaceAssets(request: Spc.StreamSpaceAssetsRequest): Flow<Spc.AssetChunk> = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.StreamSpaceAssets is unimplemented"))

    /**
     * Returns the response to an RPC for solra.spc.v1.SpcService.GetPreviewCard.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getPreviewCard(request: Spc.GetPreviewCardRequest): Spc.GetPreviewCardResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.GetPreviewCard is unimplemented"))

    /**
     * Returns the response to an RPC for solra.spc.v1.SpcService.BatchGetPreviewCards.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun batchGetPreviewCards(request: Spc.BatchGetPreviewCardsRequest): Spc.BatchGetPreviewCardsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.BatchGetPreviewCards is unimplemented"))

    /**
     * Returns the response to an RPC for solra.spc.v1.SpcService.ReportUserAction.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun reportUserAction(request: Spc.ReportUserActionRequest): Spc.ReportUserActionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.spc.v1.SpcService.ReportUserAction is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getGetSpaceMethod(),
      implementation = ::getSpace
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getListRecommendSpacesMethod(),
      implementation = ::listRecommendSpaces
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getLoadSpaceMethod(),
      implementation = ::loadSpace
    ))
      .addMethod(serverStreamingServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getStreamSpaceAssetsMethod(),
      implementation = ::streamSpaceAssets
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getGetPreviewCardMethod(),
      implementation = ::getPreviewCard
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getBatchGetPreviewCardsMethod(),
      implementation = ::batchGetPreviewCards
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpcServiceGrpc.getReportUserActionMethod(),
      implementation = ::reportUserAction
    )).build()
  }
}
