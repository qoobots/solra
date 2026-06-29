package com.solra.proto.soc

import com.solra.proto.soc.SocialServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.soc.SocialService.
 */
public object SocialServiceGrpcKt {
  public const val SERVICE_NAME: String = SocialServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val createSessionMethod:
      MethodDescriptor<Soc.CreateSessionRequest, Soc.CreateSessionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getCreateSessionMethod()

  public val joinSessionMethod: MethodDescriptor<Soc.JoinSessionRequest, Soc.JoinSessionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getJoinSessionMethod()

  public val leaveSessionMethod: MethodDescriptor<Soc.LeaveSessionRequest, Soc.LeaveSessionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getLeaveSessionMethod()

  public val getSessionMethod: MethodDescriptor<Soc.GetSessionRequest, Soc.GetSessionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getGetSessionMethod()

  public val listActiveSessionsMethod:
      MethodDescriptor<Soc.ListActiveSessionsRequest, Soc.ListActiveSessionsResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getListActiveSessionsMethod()

  public val endSessionMethod: MethodDescriptor<Soc.EndSessionRequest, Soc.EndSessionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getEndSessionMethod()

  public val getOnlineParticipantsMethod:
      MethodDescriptor<Soc.GetOnlineParticipantsRequest, Soc.GetOnlineParticipantsResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getGetOnlineParticipantsMethod()

  public val sendInteractionMethod:
      MethodDescriptor<Soc.SendInteractionRequest, Soc.SendInteractionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getSendInteractionMethod()

  public val inviteToSessionMethod:
      MethodDescriptor<Soc.InviteToSessionRequest, Soc.InviteToSessionResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getInviteToSessionMethod()

  public val followUserMethod: MethodDescriptor<Soc.FollowUserRequest, Soc.FollowUserResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getFollowUserMethod()

  public val unfollowUserMethod: MethodDescriptor<Soc.UnfollowUserRequest, Soc.UnfollowUserResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getUnfollowUserMethod()

  public val listFollowersMethod:
      MethodDescriptor<Soc.ListFollowersRequest, Soc.ListFollowersResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getListFollowersMethod()

  public val listFollowingMethod:
      MethodDescriptor<Soc.ListFollowingRequest, Soc.ListFollowingResponse>
    @JvmStatic
    get() = SocialServiceGrpc.getListFollowingMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.soc.SocialService service as suspending coroutines.
   */
  @StubFor(SocialServiceGrpc::class)
  public class SocialServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<SocialServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): SocialServiceCoroutineStub = SocialServiceCoroutineStub(channel, callOptions)

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
    public suspend fun createSession(request: Soc.CreateSessionRequest, headers: Metadata = Metadata()): Soc.CreateSessionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getCreateSessionMethod(),
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
    public suspend fun joinSession(request: Soc.JoinSessionRequest, headers: Metadata = Metadata()): Soc.JoinSessionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getJoinSessionMethod(),
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
    public suspend fun leaveSession(request: Soc.LeaveSessionRequest, headers: Metadata = Metadata()): Soc.LeaveSessionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getLeaveSessionMethod(),
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
    public suspend fun getSession(request: Soc.GetSessionRequest, headers: Metadata = Metadata()): Soc.GetSessionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getGetSessionMethod(),
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
    public suspend fun listActiveSessions(request: Soc.ListActiveSessionsRequest, headers: Metadata = Metadata()): Soc.ListActiveSessionsResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getListActiveSessionsMethod(),
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
    public suspend fun endSession(request: Soc.EndSessionRequest, headers: Metadata = Metadata()): Soc.EndSessionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getEndSessionMethod(),
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
    public suspend fun getOnlineParticipants(request: Soc.GetOnlineParticipantsRequest, headers: Metadata = Metadata()): Soc.GetOnlineParticipantsResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getGetOnlineParticipantsMethod(),
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
    public suspend fun sendInteraction(request: Soc.SendInteractionRequest, headers: Metadata = Metadata()): Soc.SendInteractionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getSendInteractionMethod(),
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
    public suspend fun inviteToSession(request: Soc.InviteToSessionRequest, headers: Metadata = Metadata()): Soc.InviteToSessionResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getInviteToSessionMethod(),
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
    public suspend fun followUser(request: Soc.FollowUserRequest, headers: Metadata = Metadata()): Soc.FollowUserResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getFollowUserMethod(),
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
    public suspend fun unfollowUser(request: Soc.UnfollowUserRequest, headers: Metadata = Metadata()): Soc.UnfollowUserResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getUnfollowUserMethod(),
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
    public suspend fun listFollowers(request: Soc.ListFollowersRequest, headers: Metadata = Metadata()): Soc.ListFollowersResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getListFollowersMethod(),
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
    public suspend fun listFollowing(request: Soc.ListFollowingRequest, headers: Metadata = Metadata()): Soc.ListFollowingResponse = unaryRpc(
      channel,
      SocialServiceGrpc.getListFollowingMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.soc.SocialService service based on Kotlin coroutines.
   */
  public abstract class SocialServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.soc.SocialService.CreateSession.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun createSession(request: Soc.CreateSessionRequest): Soc.CreateSessionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.CreateSession is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.JoinSession.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun joinSession(request: Soc.JoinSessionRequest): Soc.JoinSessionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.JoinSession is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.LeaveSession.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun leaveSession(request: Soc.LeaveSessionRequest): Soc.LeaveSessionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.LeaveSession is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.GetSession.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getSession(request: Soc.GetSessionRequest): Soc.GetSessionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.GetSession is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.ListActiveSessions.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listActiveSessions(request: Soc.ListActiveSessionsRequest): Soc.ListActiveSessionsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.ListActiveSessions is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.EndSession.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun endSession(request: Soc.EndSessionRequest): Soc.EndSessionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.EndSession is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.GetOnlineParticipants.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getOnlineParticipants(request: Soc.GetOnlineParticipantsRequest): Soc.GetOnlineParticipantsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.GetOnlineParticipants is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.SendInteraction.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun sendInteraction(request: Soc.SendInteractionRequest): Soc.SendInteractionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.SendInteraction is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.InviteToSession.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun inviteToSession(request: Soc.InviteToSessionRequest): Soc.InviteToSessionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.InviteToSession is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.FollowUser.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun followUser(request: Soc.FollowUserRequest): Soc.FollowUserResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.FollowUser is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.UnfollowUser.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun unfollowUser(request: Soc.UnfollowUserRequest): Soc.UnfollowUserResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.UnfollowUser is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.ListFollowers.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listFollowers(request: Soc.ListFollowersRequest): Soc.ListFollowersResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.ListFollowers is unimplemented"))

    /**
     * Returns the response to an RPC for solra.soc.SocialService.ListFollowing.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listFollowing(request: Soc.ListFollowingRequest): Soc.ListFollowingResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.soc.SocialService.ListFollowing is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getCreateSessionMethod(),
      implementation = ::createSession
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getJoinSessionMethod(),
      implementation = ::joinSession
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getLeaveSessionMethod(),
      implementation = ::leaveSession
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getGetSessionMethod(),
      implementation = ::getSession
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getListActiveSessionsMethod(),
      implementation = ::listActiveSessions
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getEndSessionMethod(),
      implementation = ::endSession
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getGetOnlineParticipantsMethod(),
      implementation = ::getOnlineParticipants
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getSendInteractionMethod(),
      implementation = ::sendInteraction
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getInviteToSessionMethod(),
      implementation = ::inviteToSession
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getFollowUserMethod(),
      implementation = ::followUser
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getUnfollowUserMethod(),
      implementation = ::unfollowUser
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getListFollowersMethod(),
      implementation = ::listFollowers
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SocialServiceGrpc.getListFollowingMethod(),
      implementation = ::listFollowing
    )).build()
  }
}
