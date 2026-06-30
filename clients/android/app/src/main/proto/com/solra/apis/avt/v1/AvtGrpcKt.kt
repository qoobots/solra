package com.solra.apis.avt.v1

import com.solra.apis.avt.v1.AvtServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.avt.v1.AvtService.
 */
public object AvtServiceGrpcKt {
  public const val SERVICE_NAME: String = AvtServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val sendMessageMethod: MethodDescriptor<Avt.SendMessageRequest, Avt.SendMessageResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getSendMessageMethod()

  public val streamResponseMethod: MethodDescriptor<Avt.StreamResponseRequest, Avt.DialogueTurn>
    @JvmStatic
    get() = AvtServiceGrpc.getStreamResponseMethod()

  public val getConversationHistoryMethod:
      MethodDescriptor<Avt.GetConversationHistoryRequest, Avt.GetConversationHistoryResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getGetConversationHistoryMethod()

  public val getAvatarStateMethod:
      MethodDescriptor<Avt.GetAvatarStateRequest, Avt.GetAvatarStateResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getGetAvatarStateMethod()

  public val updateEmotionStateMethod:
      MethodDescriptor<Avt.UpdateEmotionStateRequest, Avt.UpdateEmotionStateResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getUpdateEmotionStateMethod()

  public val queryMemoryMethod: MethodDescriptor<Avt.QueryMemoryRequest, Avt.QueryMemoryResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getQueryMemoryMethod()

  public val getLongTermMemoryMethod:
      MethodDescriptor<Avt.GetLongTermMemoryRequest, Avt.GetLongTermMemoryResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getGetLongTermMemoryMethod()

  public val addMemorySnapshotMethod:
      MethodDescriptor<Avt.AddMemorySnapshotRequest, Avt.AddMemorySnapshotResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getAddMemorySnapshotMethod()

  public val retrieveMemoriesMethod:
      MethodDescriptor<Avt.RetrieveMemoriesRequest, Avt.RetrieveMemoriesResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getRetrieveMemoriesMethod()

  public val getEmotionStateMethod:
      MethodDescriptor<Avt.GetEmotionStateRequest, Avt.GetEmotionStateResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getGetEmotionStateMethod()

  public val applyEmotionEventMethod:
      MethodDescriptor<Avt.ApplyEmotionEventRequest, Avt.ApplyEmotionEventResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getApplyEmotionEventMethod()

  public val decayEmotionsMethod:
      MethodDescriptor<Avt.DecayEmotionsRequest, Avt.DecayEmotionsResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getDecayEmotionsMethod()

  public val getAvatarExpressionMethod:
      MethodDescriptor<Avt.GetAvatarExpressionRequest, Avt.GetAvatarExpressionResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getGetAvatarExpressionMethod()

  public val evaluateSurpriseMethod:
      MethodDescriptor<Avt.EvaluateSurpriseRequest, Avt.EvaluateSurpriseResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getEvaluateSurpriseMethod()

  public val getSurpriseStatsMethod:
      MethodDescriptor<Avt.GetSurpriseStatsRequest, Avt.GetSurpriseStatsResponse>
    @JvmStatic
    get() = AvtServiceGrpc.getGetSurpriseStatsMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.avt.v1.AvtService service as suspending coroutines.
   */
  @StubFor(AvtServiceGrpc::class)
  public class AvtServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<AvtServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): AvtServiceCoroutineStub = AvtServiceCoroutineStub(channel, callOptions)

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
    public suspend fun sendMessage(request: Avt.SendMessageRequest, headers: Metadata = Metadata()): Avt.SendMessageResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getSendMessageMethod(),
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
    public fun streamResponse(request: Avt.StreamResponseRequest, headers: Metadata = Metadata()): Flow<Avt.DialogueTurn> = serverStreamingRpc(
      channel,
      AvtServiceGrpc.getStreamResponseMethod(),
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
    public suspend fun getConversationHistory(request: Avt.GetConversationHistoryRequest, headers: Metadata = Metadata()): Avt.GetConversationHistoryResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getGetConversationHistoryMethod(),
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
    public suspend fun getAvatarState(request: Avt.GetAvatarStateRequest, headers: Metadata = Metadata()): Avt.GetAvatarStateResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getGetAvatarStateMethod(),
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
    public suspend fun updateEmotionState(request: Avt.UpdateEmotionStateRequest, headers: Metadata = Metadata()): Avt.UpdateEmotionStateResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getUpdateEmotionStateMethod(),
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
    public suspend fun queryMemory(request: Avt.QueryMemoryRequest, headers: Metadata = Metadata()): Avt.QueryMemoryResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getQueryMemoryMethod(),
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
    public suspend fun getLongTermMemory(request: Avt.GetLongTermMemoryRequest, headers: Metadata = Metadata()): Avt.GetLongTermMemoryResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getGetLongTermMemoryMethod(),
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
    public suspend fun addMemorySnapshot(request: Avt.AddMemorySnapshotRequest, headers: Metadata = Metadata()): Avt.AddMemorySnapshotResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getAddMemorySnapshotMethod(),
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
    public suspend fun retrieveMemories(request: Avt.RetrieveMemoriesRequest, headers: Metadata = Metadata()): Avt.RetrieveMemoriesResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getRetrieveMemoriesMethod(),
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
    public suspend fun getEmotionState(request: Avt.GetEmotionStateRequest, headers: Metadata = Metadata()): Avt.GetEmotionStateResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getGetEmotionStateMethod(),
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
    public suspend fun applyEmotionEvent(request: Avt.ApplyEmotionEventRequest, headers: Metadata = Metadata()): Avt.ApplyEmotionEventResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getApplyEmotionEventMethod(),
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
    public suspend fun decayEmotions(request: Avt.DecayEmotionsRequest, headers: Metadata = Metadata()): Avt.DecayEmotionsResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getDecayEmotionsMethod(),
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
    public suspend fun getAvatarExpression(request: Avt.GetAvatarExpressionRequest, headers: Metadata = Metadata()): Avt.GetAvatarExpressionResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getGetAvatarExpressionMethod(),
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
    public suspend fun evaluateSurprise(request: Avt.EvaluateSurpriseRequest, headers: Metadata = Metadata()): Avt.EvaluateSurpriseResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getEvaluateSurpriseMethod(),
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
    public suspend fun getSurpriseStats(request: Avt.GetSurpriseStatsRequest, headers: Metadata = Metadata()): Avt.GetSurpriseStatsResponse = unaryRpc(
      channel,
      AvtServiceGrpc.getGetSurpriseStatsMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.avt.v1.AvtService service based on Kotlin coroutines.
   */
  public abstract class AvtServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.SendMessage.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun sendMessage(request: Avt.SendMessageRequest): Avt.SendMessageResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.SendMessage is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for solra.avt.v1.AvtService.StreamResponse.
     *
     * If creating or collecting the returned flow fails with a [StatusException], the RPC
     * will fail with the corresponding [io.grpc.Status].  If it fails with a
     * [java.util.concurrent.CancellationException], the RPC will fail with status `Status.CANCELLED`.  If creating
     * or collecting the returned flow fails for any other reason, the RPC will fail with
     * `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open fun streamResponse(request: Avt.StreamResponseRequest): Flow<Avt.DialogueTurn> = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.StreamResponse is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.GetConversationHistory.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getConversationHistory(request: Avt.GetConversationHistoryRequest): Avt.GetConversationHistoryResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.GetConversationHistory is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.GetAvatarState.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getAvatarState(request: Avt.GetAvatarStateRequest): Avt.GetAvatarStateResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.GetAvatarState is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.UpdateEmotionState.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateEmotionState(request: Avt.UpdateEmotionStateRequest): Avt.UpdateEmotionStateResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.UpdateEmotionState is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.QueryMemory.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun queryMemory(request: Avt.QueryMemoryRequest): Avt.QueryMemoryResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.QueryMemory is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.GetLongTermMemory.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getLongTermMemory(request: Avt.GetLongTermMemoryRequest): Avt.GetLongTermMemoryResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.GetLongTermMemory is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.AddMemorySnapshot.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun addMemorySnapshot(request: Avt.AddMemorySnapshotRequest): Avt.AddMemorySnapshotResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.AddMemorySnapshot is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.RetrieveMemories.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun retrieveMemories(request: Avt.RetrieveMemoriesRequest): Avt.RetrieveMemoriesResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.RetrieveMemories is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.GetEmotionState.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getEmotionState(request: Avt.GetEmotionStateRequest): Avt.GetEmotionStateResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.GetEmotionState is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.ApplyEmotionEvent.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun applyEmotionEvent(request: Avt.ApplyEmotionEventRequest): Avt.ApplyEmotionEventResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.ApplyEmotionEvent is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.DecayEmotions.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun decayEmotions(request: Avt.DecayEmotionsRequest): Avt.DecayEmotionsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.DecayEmotions is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.GetAvatarExpression.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getAvatarExpression(request: Avt.GetAvatarExpressionRequest): Avt.GetAvatarExpressionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.GetAvatarExpression is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.EvaluateSurprise.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun evaluateSurprise(request: Avt.EvaluateSurpriseRequest): Avt.EvaluateSurpriseResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.EvaluateSurprise is unimplemented"))

    /**
     * Returns the response to an RPC for solra.avt.v1.AvtService.GetSurpriseStats.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getSurpriseStats(request: Avt.GetSurpriseStatsRequest): Avt.GetSurpriseStatsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.avt.v1.AvtService.GetSurpriseStats is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getSendMessageMethod(),
      implementation = ::sendMessage
    ))
      .addMethod(serverStreamingServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getStreamResponseMethod(),
      implementation = ::streamResponse
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getGetConversationHistoryMethod(),
      implementation = ::getConversationHistory
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getGetAvatarStateMethod(),
      implementation = ::getAvatarState
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getUpdateEmotionStateMethod(),
      implementation = ::updateEmotionState
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getQueryMemoryMethod(),
      implementation = ::queryMemory
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getGetLongTermMemoryMethod(),
      implementation = ::getLongTermMemory
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getAddMemorySnapshotMethod(),
      implementation = ::addMemorySnapshot
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getRetrieveMemoriesMethod(),
      implementation = ::retrieveMemories
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getGetEmotionStateMethod(),
      implementation = ::getEmotionState
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getApplyEmotionEventMethod(),
      implementation = ::applyEmotionEvent
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getDecayEmotionsMethod(),
      implementation = ::decayEmotions
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getGetAvatarExpressionMethod(),
      implementation = ::getAvatarExpression
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getEvaluateSurpriseMethod(),
      implementation = ::evaluateSurprise
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AvtServiceGrpc.getGetSurpriseStatsMethod(),
      implementation = ::getSurpriseStats
    )).build()
  }
}
