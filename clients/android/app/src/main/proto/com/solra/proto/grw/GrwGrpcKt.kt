package com.solra.proto.grw

import com.solra.proto.grw.GrowthServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.grw.GrowthService.
 */
public object GrowthServiceGrpcKt {
  public const val SERVICE_NAME: String = GrowthServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val getProfileMethod: MethodDescriptor<Grw.GetProfileRequest, Grw.GetProfileResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getGetProfileMethod()

  public val updateProfileMethod:
      MethodDescriptor<Grw.UpdateProfileRequest, Grw.UpdateProfileResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getUpdateProfileMethod()

  public val getFaithLevelMethod:
      MethodDescriptor<Grw.GetFaithLevelRequest, Grw.GetFaithLevelResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getGetFaithLevelMethod()

  public val getFaithLevelHistoryMethod:
      MethodDescriptor<Grw.GetFaithLevelHistoryRequest, Grw.GetFaithLevelHistoryResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getGetFaithLevelHistoryMethod()

  public val recordExperienceMethod:
      MethodDescriptor<Grw.RecordExperienceRequest, Grw.RecordExperienceResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getRecordExperienceMethod()

  public val getExperienceHistoryMethod:
      MethodDescriptor<Grw.GetExperienceHistoryRequest, Grw.GetExperienceHistoryResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getGetExperienceHistoryMethod()

  public val listAchievementsMethod:
      MethodDescriptor<Grw.ListAchievementsRequest, Grw.ListAchievementsResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getListAchievementsMethod()

  public val getAchievementProgressMethod:
      MethodDescriptor<Grw.GetAchievementProgressRequest, Grw.GetAchievementProgressResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getGetAchievementProgressMethod()

  public val claimAchievementRewardMethod:
      MethodDescriptor<Grw.ClaimAchievementRewardRequest, Grw.ClaimAchievementRewardResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getClaimAchievementRewardMethod()

  public val listBadgesMethod: MethodDescriptor<Grw.ListBadgesRequest, Grw.ListBadgesResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getListBadgesMethod()

  public val equipBadgeMethod: MethodDescriptor<Grw.EquipBadgeRequest, Grw.EquipBadgeResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getEquipBadgeMethod()

  public val getLeaderboardMethod:
      MethodDescriptor<Grw.GetLeaderboardRequest, Grw.GetLeaderboardResponse>
    @JvmStatic
    get() = GrowthServiceGrpc.getGetLeaderboardMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.grw.GrowthService service as suspending coroutines.
   */
  @StubFor(GrowthServiceGrpc::class)
  public class GrowthServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<GrowthServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): GrowthServiceCoroutineStub = GrowthServiceCoroutineStub(channel, callOptions)

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
    public suspend fun getProfile(request: Grw.GetProfileRequest, headers: Metadata = Metadata()): Grw.GetProfileResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getGetProfileMethod(),
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
    public suspend fun updateProfile(request: Grw.UpdateProfileRequest, headers: Metadata = Metadata()): Grw.UpdateProfileResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getUpdateProfileMethod(),
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
    public suspend fun getFaithLevel(request: Grw.GetFaithLevelRequest, headers: Metadata = Metadata()): Grw.GetFaithLevelResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getGetFaithLevelMethod(),
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
    public suspend fun getFaithLevelHistory(request: Grw.GetFaithLevelHistoryRequest, headers: Metadata = Metadata()): Grw.GetFaithLevelHistoryResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getGetFaithLevelHistoryMethod(),
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
    public suspend fun recordExperience(request: Grw.RecordExperienceRequest, headers: Metadata = Metadata()): Grw.RecordExperienceResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getRecordExperienceMethod(),
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
    public suspend fun getExperienceHistory(request: Grw.GetExperienceHistoryRequest, headers: Metadata = Metadata()): Grw.GetExperienceHistoryResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getGetExperienceHistoryMethod(),
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
    public suspend fun listAchievements(request: Grw.ListAchievementsRequest, headers: Metadata = Metadata()): Grw.ListAchievementsResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getListAchievementsMethod(),
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
    public suspend fun getAchievementProgress(request: Grw.GetAchievementProgressRequest, headers: Metadata = Metadata()): Grw.GetAchievementProgressResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getGetAchievementProgressMethod(),
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
    public suspend fun claimAchievementReward(request: Grw.ClaimAchievementRewardRequest, headers: Metadata = Metadata()): Grw.ClaimAchievementRewardResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getClaimAchievementRewardMethod(),
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
    public suspend fun listBadges(request: Grw.ListBadgesRequest, headers: Metadata = Metadata()): Grw.ListBadgesResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getListBadgesMethod(),
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
    public suspend fun equipBadge(request: Grw.EquipBadgeRequest, headers: Metadata = Metadata()): Grw.EquipBadgeResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getEquipBadgeMethod(),
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
    public suspend fun getLeaderboard(request: Grw.GetLeaderboardRequest, headers: Metadata = Metadata()): Grw.GetLeaderboardResponse = unaryRpc(
      channel,
      GrowthServiceGrpc.getGetLeaderboardMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.grw.GrowthService service based on Kotlin coroutines.
   */
  public abstract class GrowthServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.grw.GrowthService.GetProfile.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getProfile(request: Grw.GetProfileRequest): Grw.GetProfileResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.GetProfile is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.UpdateProfile.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateProfile(request: Grw.UpdateProfileRequest): Grw.UpdateProfileResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.UpdateProfile is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.GetFaithLevel.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getFaithLevel(request: Grw.GetFaithLevelRequest): Grw.GetFaithLevelResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.GetFaithLevel is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.GetFaithLevelHistory.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getFaithLevelHistory(request: Grw.GetFaithLevelHistoryRequest): Grw.GetFaithLevelHistoryResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.GetFaithLevelHistory is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.RecordExperience.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun recordExperience(request: Grw.RecordExperienceRequest): Grw.RecordExperienceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.RecordExperience is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.GetExperienceHistory.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getExperienceHistory(request: Grw.GetExperienceHistoryRequest): Grw.GetExperienceHistoryResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.GetExperienceHistory is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.ListAchievements.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listAchievements(request: Grw.ListAchievementsRequest): Grw.ListAchievementsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.ListAchievements is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.GetAchievementProgress.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getAchievementProgress(request: Grw.GetAchievementProgressRequest): Grw.GetAchievementProgressResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.GetAchievementProgress is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.ClaimAchievementReward.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun claimAchievementReward(request: Grw.ClaimAchievementRewardRequest): Grw.ClaimAchievementRewardResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.ClaimAchievementReward is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.ListBadges.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listBadges(request: Grw.ListBadgesRequest): Grw.ListBadgesResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.ListBadges is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.EquipBadge.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun equipBadge(request: Grw.EquipBadgeRequest): Grw.EquipBadgeResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.EquipBadge is unimplemented"))

    /**
     * Returns the response to an RPC for solra.grw.GrowthService.GetLeaderboard.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getLeaderboard(request: Grw.GetLeaderboardRequest): Grw.GetLeaderboardResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.grw.GrowthService.GetLeaderboard is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getGetProfileMethod(),
      implementation = ::getProfile
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getUpdateProfileMethod(),
      implementation = ::updateProfile
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getGetFaithLevelMethod(),
      implementation = ::getFaithLevel
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getGetFaithLevelHistoryMethod(),
      implementation = ::getFaithLevelHistory
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getRecordExperienceMethod(),
      implementation = ::recordExperience
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getGetExperienceHistoryMethod(),
      implementation = ::getExperienceHistory
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getListAchievementsMethod(),
      implementation = ::listAchievements
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getGetAchievementProgressMethod(),
      implementation = ::getAchievementProgress
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getClaimAchievementRewardMethod(),
      implementation = ::claimAchievementReward
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getListBadgesMethod(),
      implementation = ::listBadges
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getEquipBadgeMethod(),
      implementation = ::equipBadge
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GrowthServiceGrpc.getGetLeaderboardMethod(),
      implementation = ::getLeaderboard
    )).build()
  }
}
