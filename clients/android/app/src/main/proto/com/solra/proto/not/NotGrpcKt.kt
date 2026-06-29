package com.solra.proto.not

import com.solra.proto.not.NotificationServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.not.NotificationService.
 */
public object NotificationServiceGrpcKt {
  public const val SERVICE_NAME: String = NotificationServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val sendNotificationMethod:
      MethodDescriptor<Not.SendNotificationRequest, Not.SendNotificationResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getSendNotificationMethod()

  public val listNotificationsMethod:
      MethodDescriptor<Not.ListNotificationsRequest, Not.ListNotificationsResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getListNotificationsMethod()

  public val markAsReadMethod: MethodDescriptor<Not.MarkAsReadRequest, Not.MarkAsReadResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getMarkAsReadMethod()

  public val markAllAsReadMethod:
      MethodDescriptor<Not.MarkAllAsReadRequest, Not.MarkAllAsReadResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getMarkAllAsReadMethod()

  public val deleteNotificationMethod:
      MethodDescriptor<Not.DeleteNotificationRequest, Not.DeleteNotificationResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getDeleteNotificationMethod()

  public val getUnreadCountMethod:
      MethodDescriptor<Not.GetUnreadCountRequest, Not.GetUnreadCountResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getGetUnreadCountMethod()

  public val registerDeviceMethod:
      MethodDescriptor<Not.RegisterDeviceRequest, Not.RegisterDeviceResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getRegisterDeviceMethod()

  public val unregisterDeviceMethod:
      MethodDescriptor<Not.UnregisterDeviceRequest, Not.UnregisterDeviceResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getUnregisterDeviceMethod()

  public val sendPushMessageMethod:
      MethodDescriptor<Not.SendPushMessageRequest, Not.SendPushMessageResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getSendPushMessageMethod()

  public val getInboxMethod: MethodDescriptor<Not.GetInboxRequest, Not.GetInboxResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getGetInboxMethod()

  public val sendInboxMessageMethod:
      MethodDescriptor<Not.SendInboxMessageRequest, Not.SendInboxMessageResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getSendInboxMessageMethod()

  public val listInboxMessagesMethod:
      MethodDescriptor<Not.ListInboxMessagesRequest, Not.ListInboxMessagesResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getListInboxMessagesMethod()

  public val getNotificationPreferenceMethod:
      MethodDescriptor<Not.GetNotificationPreferenceRequest, Not.GetNotificationPreferenceResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getGetNotificationPreferenceMethod()

  public val updateNotificationPreferenceMethod:
      MethodDescriptor<Not.UpdateNotificationPreferenceRequest, Not.UpdateNotificationPreferenceResponse>
    @JvmStatic
    get() = NotificationServiceGrpc.getUpdateNotificationPreferenceMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.not.NotificationService service as suspending coroutines.
   */
  @StubFor(NotificationServiceGrpc::class)
  public class NotificationServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<NotificationServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): NotificationServiceCoroutineStub = NotificationServiceCoroutineStub(channel, callOptions)

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
    public suspend fun sendNotification(request: Not.SendNotificationRequest, headers: Metadata = Metadata()): Not.SendNotificationResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getSendNotificationMethod(),
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
    public suspend fun listNotifications(request: Not.ListNotificationsRequest, headers: Metadata = Metadata()): Not.ListNotificationsResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getListNotificationsMethod(),
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
    public suspend fun markAsRead(request: Not.MarkAsReadRequest, headers: Metadata = Metadata()): Not.MarkAsReadResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getMarkAsReadMethod(),
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
    public suspend fun markAllAsRead(request: Not.MarkAllAsReadRequest, headers: Metadata = Metadata()): Not.MarkAllAsReadResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getMarkAllAsReadMethod(),
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
    public suspend fun deleteNotification(request: Not.DeleteNotificationRequest, headers: Metadata = Metadata()): Not.DeleteNotificationResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getDeleteNotificationMethod(),
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
    public suspend fun getUnreadCount(request: Not.GetUnreadCountRequest, headers: Metadata = Metadata()): Not.GetUnreadCountResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getGetUnreadCountMethod(),
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
    public suspend fun registerDevice(request: Not.RegisterDeviceRequest, headers: Metadata = Metadata()): Not.RegisterDeviceResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getRegisterDeviceMethod(),
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
    public suspend fun unregisterDevice(request: Not.UnregisterDeviceRequest, headers: Metadata = Metadata()): Not.UnregisterDeviceResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getUnregisterDeviceMethod(),
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
    public suspend fun sendPushMessage(request: Not.SendPushMessageRequest, headers: Metadata = Metadata()): Not.SendPushMessageResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getSendPushMessageMethod(),
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
    public suspend fun getInbox(request: Not.GetInboxRequest, headers: Metadata = Metadata()): Not.GetInboxResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getGetInboxMethod(),
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
    public suspend fun sendInboxMessage(request: Not.SendInboxMessageRequest, headers: Metadata = Metadata()): Not.SendInboxMessageResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getSendInboxMessageMethod(),
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
    public suspend fun listInboxMessages(request: Not.ListInboxMessagesRequest, headers: Metadata = Metadata()): Not.ListInboxMessagesResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getListInboxMessagesMethod(),
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
    public suspend fun getNotificationPreference(request: Not.GetNotificationPreferenceRequest, headers: Metadata = Metadata()): Not.GetNotificationPreferenceResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getGetNotificationPreferenceMethod(),
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
    public suspend fun updateNotificationPreference(request: Not.UpdateNotificationPreferenceRequest, headers: Metadata = Metadata()): Not.UpdateNotificationPreferenceResponse = unaryRpc(
      channel,
      NotificationServiceGrpc.getUpdateNotificationPreferenceMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.not.NotificationService service based on Kotlin coroutines.
   */
  public abstract class NotificationServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.not.NotificationService.SendNotification.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun sendNotification(request: Not.SendNotificationRequest): Not.SendNotificationResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.SendNotification is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.ListNotifications.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listNotifications(request: Not.ListNotificationsRequest): Not.ListNotificationsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.ListNotifications is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.MarkAsRead.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun markAsRead(request: Not.MarkAsReadRequest): Not.MarkAsReadResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.MarkAsRead is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.MarkAllAsRead.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun markAllAsRead(request: Not.MarkAllAsReadRequest): Not.MarkAllAsReadResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.MarkAllAsRead is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.DeleteNotification.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun deleteNotification(request: Not.DeleteNotificationRequest): Not.DeleteNotificationResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.DeleteNotification is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.GetUnreadCount.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getUnreadCount(request: Not.GetUnreadCountRequest): Not.GetUnreadCountResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.GetUnreadCount is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.RegisterDevice.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun registerDevice(request: Not.RegisterDeviceRequest): Not.RegisterDeviceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.RegisterDevice is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.UnregisterDevice.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun unregisterDevice(request: Not.UnregisterDeviceRequest): Not.UnregisterDeviceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.UnregisterDevice is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.SendPushMessage.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun sendPushMessage(request: Not.SendPushMessageRequest): Not.SendPushMessageResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.SendPushMessage is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.GetInbox.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getInbox(request: Not.GetInboxRequest): Not.GetInboxResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.GetInbox is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.SendInboxMessage.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun sendInboxMessage(request: Not.SendInboxMessageRequest): Not.SendInboxMessageResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.SendInboxMessage is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.ListInboxMessages.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listInboxMessages(request: Not.ListInboxMessagesRequest): Not.ListInboxMessagesResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.ListInboxMessages is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.GetNotificationPreference.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getNotificationPreference(request: Not.GetNotificationPreferenceRequest): Not.GetNotificationPreferenceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.GetNotificationPreference is unimplemented"))

    /**
     * Returns the response to an RPC for solra.not.NotificationService.UpdateNotificationPreference.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateNotificationPreference(request: Not.UpdateNotificationPreferenceRequest): Not.UpdateNotificationPreferenceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.not.NotificationService.UpdateNotificationPreference is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getSendNotificationMethod(),
      implementation = ::sendNotification
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getListNotificationsMethod(),
      implementation = ::listNotifications
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getMarkAsReadMethod(),
      implementation = ::markAsRead
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getMarkAllAsReadMethod(),
      implementation = ::markAllAsRead
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getDeleteNotificationMethod(),
      implementation = ::deleteNotification
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getGetUnreadCountMethod(),
      implementation = ::getUnreadCount
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getRegisterDeviceMethod(),
      implementation = ::registerDevice
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getUnregisterDeviceMethod(),
      implementation = ::unregisterDevice
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getSendPushMessageMethod(),
      implementation = ::sendPushMessage
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getGetInboxMethod(),
      implementation = ::getInbox
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getSendInboxMessageMethod(),
      implementation = ::sendInboxMessage
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getListInboxMessagesMethod(),
      implementation = ::listInboxMessages
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getGetNotificationPreferenceMethod(),
      implementation = ::getNotificationPreference
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = NotificationServiceGrpc.getUpdateNotificationPreferenceMethod(),
      implementation = ::updateNotificationPreference
    )).build()
  }
}
