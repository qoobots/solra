package com.solra.apis.auth.v1

import com.solra.apis.auth.v1.AuthServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.auth.v1.AuthService.
 */
public object AuthServiceGrpcKt {
  public const val SERVICE_NAME: String = AuthServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val registerMethod: MethodDescriptor<Auth.RegisterRequest, Auth.RegisterResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getRegisterMethod()

  public val loginMethod: MethodDescriptor<Auth.LoginRequest, Auth.LoginResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getLoginMethod()

  public val refreshTokenMethod:
      MethodDescriptor<Auth.RefreshTokenRequest, Auth.RefreshTokenResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getRefreshTokenMethod()

  public val logoutMethod: MethodDescriptor<Auth.LogoutRequest, Auth.LogoutResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getLogoutMethod()

  public val oAuthLoginMethod: MethodDescriptor<Auth.OAuthLoginRequest, Auth.OAuthLoginResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getOAuthLoginMethod()

  public val bindOAuthMethod: MethodDescriptor<Auth.BindOAuthRequest, Auth.BindOAuthResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getBindOAuthMethod()

  public val unbindOAuthMethod: MethodDescriptor<Auth.UnbindOAuthRequest, Auth.UnbindOAuthResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getUnbindOAuthMethod()

  public val getCurrentUserMethod:
      MethodDescriptor<Auth.GetCurrentUserRequest, Auth.GetCurrentUserResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getGetCurrentUserMethod()

  public val updateUserAccountMethod:
      MethodDescriptor<Auth.UpdateUserAccountRequest, Auth.UpdateUserAccountResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getUpdateUserAccountMethod()

  public val verifyTokenMethod: MethodDescriptor<Auth.VerifyTokenRequest, Auth.VerifyTokenResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getVerifyTokenMethod()

  public val checkPermissionMethod:
      MethodDescriptor<Auth.CheckPermissionRequest, Auth.CheckPermissionResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getCheckPermissionMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.auth.v1.AuthService service as suspending coroutines.
   */
  @StubFor(AuthServiceGrpc::class)
  public class AuthServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<AuthServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): AuthServiceCoroutineStub = AuthServiceCoroutineStub(channel, callOptions)

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
    public suspend fun register(request: Auth.RegisterRequest, headers: Metadata = Metadata()): Auth.RegisterResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getRegisterMethod(),
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
    public suspend fun login(request: Auth.LoginRequest, headers: Metadata = Metadata()): Auth.LoginResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getLoginMethod(),
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
    public suspend fun refreshToken(request: Auth.RefreshTokenRequest, headers: Metadata = Metadata()): Auth.RefreshTokenResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getRefreshTokenMethod(),
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
    public suspend fun logout(request: Auth.LogoutRequest, headers: Metadata = Metadata()): Auth.LogoutResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getLogoutMethod(),
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
    public suspend fun oAuthLogin(request: Auth.OAuthLoginRequest, headers: Metadata = Metadata()): Auth.OAuthLoginResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getOAuthLoginMethod(),
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
    public suspend fun bindOAuth(request: Auth.BindOAuthRequest, headers: Metadata = Metadata()): Auth.BindOAuthResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getBindOAuthMethod(),
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
    public suspend fun unbindOAuth(request: Auth.UnbindOAuthRequest, headers: Metadata = Metadata()): Auth.UnbindOAuthResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getUnbindOAuthMethod(),
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
    public suspend fun getCurrentUser(request: Auth.GetCurrentUserRequest, headers: Metadata = Metadata()): Auth.GetCurrentUserResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getGetCurrentUserMethod(),
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
    public suspend fun updateUserAccount(request: Auth.UpdateUserAccountRequest, headers: Metadata = Metadata()): Auth.UpdateUserAccountResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getUpdateUserAccountMethod(),
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
    public suspend fun verifyToken(request: Auth.VerifyTokenRequest, headers: Metadata = Metadata()): Auth.VerifyTokenResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getVerifyTokenMethod(),
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
    public suspend fun checkPermission(request: Auth.CheckPermissionRequest, headers: Metadata = Metadata()): Auth.CheckPermissionResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getCheckPermissionMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.auth.v1.AuthService service based on Kotlin coroutines.
   */
  public abstract class AuthServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.Register.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun register(request: Auth.RegisterRequest): Auth.RegisterResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.Register is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.Login.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun login(request: Auth.LoginRequest): Auth.LoginResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.Login is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.RefreshToken.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun refreshToken(request: Auth.RefreshTokenRequest): Auth.RefreshTokenResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.RefreshToken is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.Logout.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun logout(request: Auth.LogoutRequest): Auth.LogoutResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.Logout is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.OAuthLogin.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun oAuthLogin(request: Auth.OAuthLoginRequest): Auth.OAuthLoginResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.OAuthLogin is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.BindOAuth.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun bindOAuth(request: Auth.BindOAuthRequest): Auth.BindOAuthResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.BindOAuth is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.UnbindOAuth.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun unbindOAuth(request: Auth.UnbindOAuthRequest): Auth.UnbindOAuthResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.UnbindOAuth is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.GetCurrentUser.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getCurrentUser(request: Auth.GetCurrentUserRequest): Auth.GetCurrentUserResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.GetCurrentUser is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.UpdateUserAccount.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateUserAccount(request: Auth.UpdateUserAccountRequest): Auth.UpdateUserAccountResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.UpdateUserAccount is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.VerifyToken.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun verifyToken(request: Auth.VerifyTokenRequest): Auth.VerifyTokenResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.VerifyToken is unimplemented"))

    /**
     * Returns the response to an RPC for solra.auth.v1.AuthService.CheckPermission.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun checkPermission(request: Auth.CheckPermissionRequest): Auth.CheckPermissionResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.auth.v1.AuthService.CheckPermission is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getRegisterMethod(),
      implementation = ::register
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getLoginMethod(),
      implementation = ::login
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getRefreshTokenMethod(),
      implementation = ::refreshToken
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getLogoutMethod(),
      implementation = ::logout
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getOAuthLoginMethod(),
      implementation = ::oAuthLogin
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getBindOAuthMethod(),
      implementation = ::bindOAuth
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getUnbindOAuthMethod(),
      implementation = ::unbindOAuth
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getGetCurrentUserMethod(),
      implementation = ::getCurrentUser
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getUpdateUserAccountMethod(),
      implementation = ::updateUserAccount
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getVerifyTokenMethod(),
      implementation = ::verifyToken
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getCheckPermissionMethod(),
      implementation = ::checkPermission
    )).build()
  }
}
