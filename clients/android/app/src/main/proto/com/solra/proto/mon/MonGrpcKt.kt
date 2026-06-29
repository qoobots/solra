package com.solra.proto.mon

import com.solra.apis.common.Common
import com.solra.proto.mon.MonServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.mon.MonService.
 */
public object MonServiceGrpcKt {
  public const val SERVICE_NAME: String = MonServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val getWalletMethod: MethodDescriptor<Mon.GetWalletRequest, Mon.GetWalletResponse>
    @JvmStatic
    get() = MonServiceGrpc.getGetWalletMethod()

  public val addBalanceMethod: MethodDescriptor<Mon.AddBalanceRequest, Mon.AddBalanceResponse>
    @JvmStatic
    get() = MonServiceGrpc.getAddBalanceMethod()

  public val deductBalanceMethod:
      MethodDescriptor<Mon.DeductBalanceRequest, Mon.DeductBalanceResponse>
    @JvmStatic
    get() = MonServiceGrpc.getDeductBalanceMethod()

  public val createOrderMethod: MethodDescriptor<Mon.CreateOrderRequest, Mon.CreateOrderResponse>
    @JvmStatic
    get() = MonServiceGrpc.getCreateOrderMethod()

  public val getOrderMethod: MethodDescriptor<Mon.GetOrderRequest, Mon.Order>
    @JvmStatic
    get() = MonServiceGrpc.getGetOrderMethod()

  public val verifyPaymentMethod:
      MethodDescriptor<Mon.VerifyPaymentRequest, Mon.VerifyPaymentResponse>
    @JvmStatic
    get() = MonServiceGrpc.getVerifyPaymentMethod()

  public val listSubscriptionPlansMethod:
      MethodDescriptor<Common.Empty, Mon.ListSubscriptionPlansResponse>
    @JvmStatic
    get() = MonServiceGrpc.getListSubscriptionPlansMethod()

  public val subscribeMethod: MethodDescriptor<Mon.SubscribeRequest, Mon.SubscribeResponse>
    @JvmStatic
    get() = MonServiceGrpc.getSubscribeMethod()

  public val cancelSubscriptionMethod:
      MethodDescriptor<Mon.CancelSubscriptionRequest, Mon.Subscription>
    @JvmStatic
    get() = MonServiceGrpc.getCancelSubscriptionMethod()

  public val listVirtualItemsMethod:
      MethodDescriptor<Mon.ListVirtualItemsRequest, Mon.ListVirtualItemsResponse>
    @JvmStatic
    get() = MonServiceGrpc.getListVirtualItemsMethod()

  public val purchaseItemMethod: MethodDescriptor<Mon.PurchaseItemRequest, Mon.PurchaseItemResponse>
    @JvmStatic
    get() = MonServiceGrpc.getPurchaseItemMethod()

  public val getUserInventoryMethod:
      MethodDescriptor<Mon.GetUserInventoryRequest, Mon.UserInventory>
    @JvmStatic
    get() = MonServiceGrpc.getGetUserInventoryMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.mon.MonService service as suspending coroutines.
   */
  @StubFor(MonServiceGrpc::class)
  public class MonServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<MonServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): MonServiceCoroutineStub = MonServiceCoroutineStub(channel, callOptions)

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
    public suspend fun getWallet(request: Mon.GetWalletRequest, headers: Metadata = Metadata()): Mon.GetWalletResponse = unaryRpc(
      channel,
      MonServiceGrpc.getGetWalletMethod(),
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
    public suspend fun addBalance(request: Mon.AddBalanceRequest, headers: Metadata = Metadata()): Mon.AddBalanceResponse = unaryRpc(
      channel,
      MonServiceGrpc.getAddBalanceMethod(),
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
    public suspend fun deductBalance(request: Mon.DeductBalanceRequest, headers: Metadata = Metadata()): Mon.DeductBalanceResponse = unaryRpc(
      channel,
      MonServiceGrpc.getDeductBalanceMethod(),
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
    public suspend fun createOrder(request: Mon.CreateOrderRequest, headers: Metadata = Metadata()): Mon.CreateOrderResponse = unaryRpc(
      channel,
      MonServiceGrpc.getCreateOrderMethod(),
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
    public suspend fun getOrder(request: Mon.GetOrderRequest, headers: Metadata = Metadata()): Mon.Order = unaryRpc(
      channel,
      MonServiceGrpc.getGetOrderMethod(),
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
    public suspend fun verifyPayment(request: Mon.VerifyPaymentRequest, headers: Metadata = Metadata()): Mon.VerifyPaymentResponse = unaryRpc(
      channel,
      MonServiceGrpc.getVerifyPaymentMethod(),
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
    public suspend fun listSubscriptionPlans(request: Common.Empty, headers: Metadata = Metadata()): Mon.ListSubscriptionPlansResponse = unaryRpc(
      channel,
      MonServiceGrpc.getListSubscriptionPlansMethod(),
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
    public suspend fun subscribe(request: Mon.SubscribeRequest, headers: Metadata = Metadata()): Mon.SubscribeResponse = unaryRpc(
      channel,
      MonServiceGrpc.getSubscribeMethod(),
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
    public suspend fun cancelSubscription(request: Mon.CancelSubscriptionRequest, headers: Metadata = Metadata()): Mon.Subscription = unaryRpc(
      channel,
      MonServiceGrpc.getCancelSubscriptionMethod(),
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
    public suspend fun listVirtualItems(request: Mon.ListVirtualItemsRequest, headers: Metadata = Metadata()): Mon.ListVirtualItemsResponse = unaryRpc(
      channel,
      MonServiceGrpc.getListVirtualItemsMethod(),
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
    public suspend fun purchaseItem(request: Mon.PurchaseItemRequest, headers: Metadata = Metadata()): Mon.PurchaseItemResponse = unaryRpc(
      channel,
      MonServiceGrpc.getPurchaseItemMethod(),
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
    public suspend fun getUserInventory(request: Mon.GetUserInventoryRequest, headers: Metadata = Metadata()): Mon.UserInventory = unaryRpc(
      channel,
      MonServiceGrpc.getGetUserInventoryMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.mon.MonService service based on Kotlin coroutines.
   */
  public abstract class MonServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.mon.MonService.GetWallet.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getWallet(request: Mon.GetWalletRequest): Mon.GetWalletResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.GetWallet is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.AddBalance.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun addBalance(request: Mon.AddBalanceRequest): Mon.AddBalanceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.AddBalance is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.DeductBalance.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun deductBalance(request: Mon.DeductBalanceRequest): Mon.DeductBalanceResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.DeductBalance is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.CreateOrder.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun createOrder(request: Mon.CreateOrderRequest): Mon.CreateOrderResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.CreateOrder is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.GetOrder.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getOrder(request: Mon.GetOrderRequest): Mon.Order = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.GetOrder is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.VerifyPayment.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun verifyPayment(request: Mon.VerifyPaymentRequest): Mon.VerifyPaymentResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.VerifyPayment is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.ListSubscriptionPlans.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listSubscriptionPlans(request: Common.Empty): Mon.ListSubscriptionPlansResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.ListSubscriptionPlans is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.Subscribe.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun subscribe(request: Mon.SubscribeRequest): Mon.SubscribeResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.Subscribe is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.CancelSubscription.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun cancelSubscription(request: Mon.CancelSubscriptionRequest): Mon.Subscription = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.CancelSubscription is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.ListVirtualItems.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listVirtualItems(request: Mon.ListVirtualItemsRequest): Mon.ListVirtualItemsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.ListVirtualItems is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.PurchaseItem.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun purchaseItem(request: Mon.PurchaseItemRequest): Mon.PurchaseItemResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.PurchaseItem is unimplemented"))

    /**
     * Returns the response to an RPC for solra.mon.MonService.GetUserInventory.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getUserInventory(request: Mon.GetUserInventoryRequest): Mon.UserInventory = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.mon.MonService.GetUserInventory is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getGetWalletMethod(),
      implementation = ::getWallet
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getAddBalanceMethod(),
      implementation = ::addBalance
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getDeductBalanceMethod(),
      implementation = ::deductBalance
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getCreateOrderMethod(),
      implementation = ::createOrder
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getGetOrderMethod(),
      implementation = ::getOrder
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getVerifyPaymentMethod(),
      implementation = ::verifyPayment
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getListSubscriptionPlansMethod(),
      implementation = ::listSubscriptionPlans
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getSubscribeMethod(),
      implementation = ::subscribe
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getCancelSubscriptionMethod(),
      implementation = ::cancelSubscription
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getListVirtualItemsMethod(),
      implementation = ::listVirtualItems
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getPurchaseItemMethod(),
      implementation = ::purchaseItem
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MonServiceGrpc.getGetUserInventoryMethod(),
      implementation = ::getUserInventory
    )).build()
  }
}
