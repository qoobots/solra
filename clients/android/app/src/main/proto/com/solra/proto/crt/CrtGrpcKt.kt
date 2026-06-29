package com.solra.proto.crt

import com.solra.proto.crt.SpaceCreationServiceGrpc.getServiceDescriptor
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
 * Holder for Kotlin coroutine-based client and server APIs for solra.crt.SpaceCreationService.
 */
public object SpaceCreationServiceGrpcKt {
  public const val SERVICE_NAME: String = SpaceCreationServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = getServiceDescriptor()

  public val createProjectMethod:
      MethodDescriptor<Crt.CreateProjectRequest, Crt.CreateProjectResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getCreateProjectMethod()

  public val getProjectMethod: MethodDescriptor<Crt.GetProjectRequest, Crt.GetProjectResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getGetProjectMethod()

  public val updateProjectMethod:
      MethodDescriptor<Crt.UpdateProjectRequest, Crt.UpdateProjectResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getUpdateProjectMethod()

  public val deleteProjectMethod:
      MethodDescriptor<Crt.DeleteProjectRequest, Crt.DeleteProjectResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getDeleteProjectMethod()

  public val listProjectsMethod: MethodDescriptor<Crt.ListProjectsRequest, Crt.ListProjectsResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getListProjectsMethod()

  public val uploadAssetMethod: MethodDescriptor<Crt.UploadAssetRequest, Crt.UploadAssetResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getUploadAssetMethod()

  public val getAssetMethod: MethodDescriptor<Crt.GetAssetRequest, Crt.GetAssetResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getGetAssetMethod()

  public val listAssetsMethod: MethodDescriptor<Crt.ListAssetsRequest, Crt.ListAssetsResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getListAssetsMethod()

  public val deleteAssetMethod: MethodDescriptor<Crt.DeleteAssetRequest, Crt.DeleteAssetResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getDeleteAssetMethod()

  public val updateSceneGraphMethod:
      MethodDescriptor<Crt.UpdateSceneGraphRequest, Crt.UpdateSceneGraphResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getUpdateSceneGraphMethod()

  public val getSceneGraphMethod:
      MethodDescriptor<Crt.GetSceneGraphRequest, Crt.GetSceneGraphResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getGetSceneGraphMethod()

  public val listTemplatesMethod:
      MethodDescriptor<Crt.ListTemplatesRequest, Crt.ListTemplatesResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getListTemplatesMethod()

  public val useTemplateMethod: MethodDescriptor<Crt.UseTemplateRequest, Crt.UseTemplateResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getUseTemplateMethod()

  public val buildProjectMethod: MethodDescriptor<Crt.BuildProjectRequest, Crt.BuildProjectResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getBuildProjectMethod()

  public val publishProjectMethod:
      MethodDescriptor<Crt.PublishProjectRequest, Crt.PublishProjectResponse>
    @JvmStatic
    get() = SpaceCreationServiceGrpc.getPublishProjectMethod()

  /**
   * A stub for issuing RPCs to a(n) solra.crt.SpaceCreationService service as suspending coroutines.
   */
  @StubFor(SpaceCreationServiceGrpc::class)
  public class SpaceCreationServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<SpaceCreationServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): SpaceCreationServiceCoroutineStub = SpaceCreationServiceCoroutineStub(channel, callOptions)

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
    public suspend fun createProject(request: Crt.CreateProjectRequest, headers: Metadata = Metadata()): Crt.CreateProjectResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getCreateProjectMethod(),
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
    public suspend fun getProject(request: Crt.GetProjectRequest, headers: Metadata = Metadata()): Crt.GetProjectResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getGetProjectMethod(),
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
    public suspend fun updateProject(request: Crt.UpdateProjectRequest, headers: Metadata = Metadata()): Crt.UpdateProjectResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getUpdateProjectMethod(),
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
    public suspend fun deleteProject(request: Crt.DeleteProjectRequest, headers: Metadata = Metadata()): Crt.DeleteProjectResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getDeleteProjectMethod(),
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
    public suspend fun listProjects(request: Crt.ListProjectsRequest, headers: Metadata = Metadata()): Crt.ListProjectsResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getListProjectsMethod(),
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
    public suspend fun uploadAsset(request: Crt.UploadAssetRequest, headers: Metadata = Metadata()): Crt.UploadAssetResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getUploadAssetMethod(),
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
    public suspend fun getAsset(request: Crt.GetAssetRequest, headers: Metadata = Metadata()): Crt.GetAssetResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getGetAssetMethod(),
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
    public suspend fun listAssets(request: Crt.ListAssetsRequest, headers: Metadata = Metadata()): Crt.ListAssetsResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getListAssetsMethod(),
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
    public suspend fun deleteAsset(request: Crt.DeleteAssetRequest, headers: Metadata = Metadata()): Crt.DeleteAssetResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getDeleteAssetMethod(),
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
    public suspend fun updateSceneGraph(request: Crt.UpdateSceneGraphRequest, headers: Metadata = Metadata()): Crt.UpdateSceneGraphResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getUpdateSceneGraphMethod(),
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
    public suspend fun getSceneGraph(request: Crt.GetSceneGraphRequest, headers: Metadata = Metadata()): Crt.GetSceneGraphResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getGetSceneGraphMethod(),
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
    public suspend fun listTemplates(request: Crt.ListTemplatesRequest, headers: Metadata = Metadata()): Crt.ListTemplatesResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getListTemplatesMethod(),
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
    public suspend fun useTemplate(request: Crt.UseTemplateRequest, headers: Metadata = Metadata()): Crt.UseTemplateResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getUseTemplateMethod(),
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
    public suspend fun buildProject(request: Crt.BuildProjectRequest, headers: Metadata = Metadata()): Crt.BuildProjectResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getBuildProjectMethod(),
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
    public suspend fun publishProject(request: Crt.PublishProjectRequest, headers: Metadata = Metadata()): Crt.PublishProjectResponse = unaryRpc(
      channel,
      SpaceCreationServiceGrpc.getPublishProjectMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the solra.crt.SpaceCreationService service based on Kotlin coroutines.
   */
  public abstract class SpaceCreationServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.CreateProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun createProject(request: Crt.CreateProjectRequest): Crt.CreateProjectResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.CreateProject is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.GetProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getProject(request: Crt.GetProjectRequest): Crt.GetProjectResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.GetProject is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.UpdateProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateProject(request: Crt.UpdateProjectRequest): Crt.UpdateProjectResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.UpdateProject is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.DeleteProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun deleteProject(request: Crt.DeleteProjectRequest): Crt.DeleteProjectResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.DeleteProject is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.ListProjects.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listProjects(request: Crt.ListProjectsRequest): Crt.ListProjectsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.ListProjects is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.UploadAsset.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun uploadAsset(request: Crt.UploadAssetRequest): Crt.UploadAssetResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.UploadAsset is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.GetAsset.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getAsset(request: Crt.GetAssetRequest): Crt.GetAssetResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.GetAsset is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.ListAssets.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listAssets(request: Crt.ListAssetsRequest): Crt.ListAssetsResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.ListAssets is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.DeleteAsset.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun deleteAsset(request: Crt.DeleteAssetRequest): Crt.DeleteAssetResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.DeleteAsset is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.UpdateSceneGraph.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateSceneGraph(request: Crt.UpdateSceneGraphRequest): Crt.UpdateSceneGraphResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.UpdateSceneGraph is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.GetSceneGraph.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getSceneGraph(request: Crt.GetSceneGraphRequest): Crt.GetSceneGraphResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.GetSceneGraph is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.ListTemplates.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listTemplates(request: Crt.ListTemplatesRequest): Crt.ListTemplatesResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.ListTemplates is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.UseTemplate.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun useTemplate(request: Crt.UseTemplateRequest): Crt.UseTemplateResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.UseTemplate is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.BuildProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun buildProject(request: Crt.BuildProjectRequest): Crt.BuildProjectResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.BuildProject is unimplemented"))

    /**
     * Returns the response to an RPC for solra.crt.SpaceCreationService.PublishProject.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun publishProject(request: Crt.PublishProjectRequest): Crt.PublishProjectResponse = throw StatusException(UNIMPLEMENTED.withDescription("Method solra.crt.SpaceCreationService.PublishProject is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getCreateProjectMethod(),
      implementation = ::createProject
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getGetProjectMethod(),
      implementation = ::getProject
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getUpdateProjectMethod(),
      implementation = ::updateProject
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getDeleteProjectMethod(),
      implementation = ::deleteProject
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getListProjectsMethod(),
      implementation = ::listProjects
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getUploadAssetMethod(),
      implementation = ::uploadAsset
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getGetAssetMethod(),
      implementation = ::getAsset
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getListAssetsMethod(),
      implementation = ::listAssets
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getDeleteAssetMethod(),
      implementation = ::deleteAsset
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getUpdateSceneGraphMethod(),
      implementation = ::updateSceneGraph
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getGetSceneGraphMethod(),
      implementation = ::getSceneGraph
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getListTemplatesMethod(),
      implementation = ::listTemplates
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getUseTemplateMethod(),
      implementation = ::useTemplate
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getBuildProjectMethod(),
      implementation = ::buildProject
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = SpaceCreationServiceGrpc.getPublishProjectMethod(),
      implementation = ::publishProject
    )).build()
  }
}
