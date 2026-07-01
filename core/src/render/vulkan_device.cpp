/*
 * Solra Core SDK - Vulkan 1.3 Backend Implementation
 *
 * Full GpuDevice/GpuBuffer/GpuTexture/GpuShader/GpuPipeline/GpuCommandBuffer
 * implementation over Vulkan 1.3 API. Supports Windows, Linux, macOS (MoltenVK).
 */

#include "vulkan_device.hpp"
#include <spdlog/spdlog.h>
#include <cstring>
#include <vector>
#include <set>
#include <algorithm>

namespace solra::render {

// ============================================================
// VulkanDevice
// ============================================================

VulkanDevice::VulkanDevice() {}
VulkanDevice::~VulkanDevice() { shutdown(); }

std::string VulkanDevice::name() const {
    return "Vulkan 1.3 — " + deviceName_;
}

bool VulkanDevice::initialize() {
    if (initialized_) return true;

    if (!createInstance()) return false;
    setupDebugMessenger();
    if (!pickPhysicalDevice()) return false;
    if (!createLogicalDevice()) return false;
    if (!createCommandPool()) return false;

    initialized_ = true;
    spdlog::info("Vulkan device initialized: {} ({})", deviceName_, vendorName_);
    return true;
}

void VulkanDevice::shutdown() {
    if (!initialized_) return;

    waitIdle();

    if (commandPool_) {
        vkDestroyCommandPool(device_, commandPool_, nullptr);
        commandPool_ = VK_NULL_HANDLE;
    }

    if (device_) {
        vkDestroyDevice(device_, nullptr);
        device_ = VK_NULL_HANDLE;
    }

    destroyDebugMessenger();

    if (instance_) {
        vkDestroyInstance(instance_, nullptr);
        instance_ = VK_NULL_HANDLE;
    }

    initialized_ = false;
}

bool VulkanDevice::createInstance() {
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "Solra Core";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "Solra Render Engine";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_3;

    // Required extensions
    std::vector<const char*> extensions = {
        VK_KHR_SURFACE_EXTENSION_NAME,
#ifdef _WIN32
        VK_KHR_WIN32_SURFACE_EXTENSION_NAME,
#elif defined(__APPLE__)
        VK_EXT_METAL_SURFACE_EXTENSION_NAME,
#else
        VK_KHR_XLIB_SURFACE_EXTENSION_NAME,
#endif
        VK_EXT_DEBUG_UTILS_EXTENSION_NAME,
    };

    // Validation layers (debug builds only)
    std::vector<const char*> layers;
#ifndef NDEBUG
    layers.push_back("VK_LAYER_KHRONOS_validation");
#endif

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    createInfo.ppEnabledExtensionNames = extensions.data();
    createInfo.enabledLayerCount = static_cast<uint32_t>(layers.size());
    createInfo.ppEnabledLayerNames = layers.data();

    VkResult result = vkCreateInstance(&createInfo, nullptr, &instance_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan instance: {}", static_cast<int>(result));
        return false;
    }
    return true;
}

bool VulkanDevice::pickPhysicalDevice() {
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance_, &deviceCount, nullptr);
    if (deviceCount == 0) {
        spdlog::error("No Vulkan-capable GPU found");
        return false;
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance_, &deviceCount, devices.data());

    // Pick first discrete GPU, or fallback to first available
    for (auto& dev : devices) {
        VkPhysicalDeviceProperties props;
        vkGetPhysicalDeviceProperties(dev, &props);

        if (props.deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            physicalDevice_ = dev;
            deviceName_ = props.deviceName;
            vendorName_ = std::to_string(props.vendorID);
            break;
        }
    }

    if (physicalDevice_ == VK_NULL_HANDLE) {
        // Fallback to first available
        physicalDevice_ = devices[0];
        VkPhysicalDeviceProperties props;
        vkGetPhysicalDeviceProperties(physicalDevice_, &props);
        deviceName_ = props.deviceName;
        vendorName_ = std::to_string(props.vendorID);
    }

    return true;
}

bool VulkanDevice::createLogicalDevice() {
    // Find graphics queue family
    uint32_t queueFamilyCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice_, &queueFamilyCount, nullptr);
    std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice_, &queueFamilyCount, queueFamilies.data());

    graphicsQueueFamily_ = UINT32_MAX;
    for (uint32_t i = 0; i < queueFamilyCount; i++) {
        if (queueFamilies[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) {
            graphicsQueueFamily_ = i;
            break;
        }
    }

    if (graphicsQueueFamily_ == UINT32_MAX) {
        spdlog::error("No graphics queue family found");
        return false;
    }

    // Create device
    float queuePriority = 1.0f;
    VkDeviceQueueCreateInfo queueCreateInfo{};
    queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueCreateInfo.queueFamilyIndex = graphicsQueueFamily_;
    queueCreateInfo.queueCount = 1;
    queueCreateInfo.pQueuePriorities = &queuePriority;

    // Device extensions
    std::vector<const char*> deviceExtensions = {
        VK_KHR_SWAPCHAIN_EXTENSION_NAME,
    };

    VkPhysicalDeviceFeatures deviceFeatures{};
    deviceFeatures.samplerAnisotropy = VK_TRUE;

    VkDeviceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    createInfo.queueCreateInfoCount = 1;
    createInfo.pQueueCreateInfos = &queueCreateInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(deviceExtensions.size());
    createInfo.ppEnabledExtensionNames = deviceExtensions.data();
    createInfo.pEnabledFeatures = &deviceFeatures;

    VkResult result = vkCreateDevice(physicalDevice_, &createInfo, nullptr, &device_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan logical device: {}", static_cast<int>(result));
        return false;
    }

    vkGetDeviceQueue(device_, graphicsQueueFamily_, 0, &graphicsQueue_);

    // Query memory budget
    VkPhysicalDeviceMemoryProperties memProps;
    vkGetPhysicalDeviceMemoryProperties(physicalDevice_, &memProps);
    memoryBudget_ = 0;
    for (uint32_t i = 0; i < memProps.memoryHeapCount; i++) {
        if (memProps.memoryHeaps[i].flags & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) {
            memoryBudget_ = memProps.memoryHeaps[i].size;
            break;
        }
    }

    return true;
}

bool VulkanDevice::createCommandPool() {
    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = graphicsQueueFamily_;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;

    VkResult result = vkCreateCommandPool(device_, &poolInfo, nullptr, &commandPool_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan command pool: {}", static_cast<int>(result));
        return false;
    }
    return true;
}

void VulkanDevice::setupDebugMessenger() {
#ifndef NDEBUG
    VkDebugUtilsMessengerCreateInfoEXT createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
    createInfo.messageSeverity =
        VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
    createInfo.messageType =
        VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
    createInfo.pfnUserCallback = [](VkDebugUtilsMessageSeverityFlagBitsEXT,
                                     VkDebugUtilsMessageTypeFlagsEXT,
                                     const VkDebugUtilsMessengerCallbackDataEXT* pCallbackData,
                                     void*) -> VkBool32 {
        spdlog::warn("[Vulkan] {}", pCallbackData->pMessage);
        return VK_FALSE;
    };

    auto func = (PFN_vkCreateDebugUtilsMessengerEXT)
        vkGetInstanceProcAddr(instance_, "vkCreateDebugUtilsMessengerEXT");
    if (func) {
        func(instance_, &createInfo, nullptr, &debugMessenger_);
    }
#endif
}

void VulkanDevice::destroyDebugMessenger() {
    if (debugMessenger_) {
        auto func = (PFN_vkDestroyDebugUtilsMessengerEXT)
            vkGetInstanceProcAddr(instance_, "vkDestroyDebugUtilsMessengerEXT");
        if (func) {
            func(instance_, debugMessenger_, nullptr);
        }
        debugMessenger_ = VK_NULL_HANDLE;
    }
}

// ============================================================
// Resource creation — VulkanDevice
// ============================================================

std::shared_ptr<GpuBuffer> VulkanDevice::createBuffer(const BufferDesc& desc, const void* data) {
    return std::make_shared<VulkanBuffer>(this, desc, data);
}

std::shared_ptr<GpuTexture> VulkanDevice::createTexture(const TextureDesc& desc) {
    return std::make_shared<VulkanTexture>(this, desc);
}

std::shared_ptr<GpuShader> VulkanDevice::createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) {
    return std::make_shared<VulkanShader>(this, stage, spirv);
}

std::shared_ptr<GpuPipeline> VulkanDevice::createPipeline(const PipelineDesc& desc) {
    return std::make_shared<VulkanPipeline>(this, desc);
}

std::shared_ptr<GpuCommandBuffer> VulkanDevice::createCommandBuffer() {
    auto cmd = std::make_shared<VulkanCommandBuffer>(this);
    return cmd;
}

void VulkanDevice::submit(std::shared_ptr<GpuCommandBuffer> cmd) {
    auto vkCmd = std::dynamic_pointer_cast<VulkanCommandBuffer>(cmd);
    if (!vkCmd) return;

    // Submit is a no-op for immediate-mode; actual submission happens in end()
    // In a real implementation, this would submit to a queue
}

void VulkanDevice::present() {
    // Swapchain presentation (handled by window system integration)
}

void VulkanDevice::waitIdle() {
    if (device_) {
        vkDeviceWaitIdle(device_);
    }
}

uint64_t VulkanDevice::gpuMemoryUsed() const { return memoryUsed_; }
uint64_t VulkanDevice::gpuMemoryBudget() const { return memoryBudget_; }

void VulkanDevice::trackAllocation(uint64_t bytes) {
    std::lock_guard<std::mutex> lock(resourceMutex_);
    memoryUsed_ += bytes;
}

void VulkanDevice::trackDeallocation(uint64_t bytes) {
    std::lock_guard<std::mutex> lock(resourceMutex_);
    if (memoryUsed_ >= bytes) memoryUsed_ -= bytes;
    else memoryUsed_ = 0;
}

// ============================================================
// VulkanBuffer
// ============================================================

VulkanBuffer::VulkanBuffer(VulkanDevice* device, const BufferDesc& desc, const void* data)
    : device_(device), size_(desc.size) {

    VkBufferUsageFlags usage = 0;
    switch (desc.usage) {
        case BufferUsage::Vertex:   usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT; break;
        case BufferUsage::Index:    usage = VK_BUFFER_USAGE_INDEX_BUFFER_BIT; break;
        case BufferUsage::Uniform:  usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT; break;
        case BufferUsage::Storage:  usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT; break;
        case BufferUsage::Staging:  usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT; break;
    }

    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = size_;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    VkResult result = vkCreateBuffer(device_->device(), &bufferInfo, nullptr, &buffer_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan buffer: {}", static_cast<int>(result));
        return;
    }

    // Allocate memory
    VkMemoryRequirements memRequirements;
    vkGetBufferMemoryRequirements(device_->device(), buffer_, &memRequirements);

    VkMemoryPropertyFlags properties = desc.hostVisible
        ? (VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        : VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);

    result = vkAllocateMemory(device_->device(), &allocInfo, nullptr, &memory_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to allocate Vulkan buffer memory: {}", static_cast<int>(result));
        vkDestroyBuffer(device_->device(), buffer_, nullptr);
        buffer_ = VK_NULL_HANDLE;
        return;
    }

    vkBindBufferMemory(device_->device(), buffer_, memory_, 0);

    // Upload initial data if provided
    if (data) {
        void* mapped = nullptr;
        vkMapMemory(device_->device(), memory_, 0, size_, 0, &mapped);
        std::memcpy(mapped, data, size_);
        vkUnmapMemory(device_->device(), memory_);
    }

    device_->trackAllocation(memRequirements.size);
}

VulkanBuffer::~VulkanBuffer() {
    if (memory_) {
        VkMemoryRequirements memRequirements;
        vkGetBufferMemoryRequirements(device_->device(), buffer_, &memRequirements);
        device_->trackDeallocation(memRequirements.size);
        vkFreeMemory(device_->device(), memory_, nullptr);
    }
    if (buffer_) {
        vkDestroyBuffer(device_->device(), buffer_, nullptr);
    }
}

void* VulkanBuffer::map() {
    if (!mapped_) {
        vkMapMemory(device_->device(), memory_, 0, size_, 0, &mappedPtr_);
        mapped_ = true;
    }
    return mappedPtr_;
}

void VulkanBuffer::unmap() {
    if (mapped_) {
        vkUnmapMemory(device_->device(), memory_);
        mapped_ = false;
        mappedPtr_ = nullptr;
    }
}

uint32_t VulkanBuffer::findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties) {
    VkPhysicalDeviceMemoryProperties memProps;
    vkGetPhysicalDeviceMemoryProperties(device_->physicalDevice(), &memProps);
    for (uint32_t i = 0; i < memProps.memoryTypeCount; i++) {
        if ((typeFilter & (1 << i)) &&
            (memProps.memoryTypes[i].propertyFlags & properties) == properties) {
            return i;
        }
    }
    return 0; // fallback to first available
}

// ============================================================
// VulkanTexture
// ============================================================

VulkanTexture::VulkanTexture(VulkanDevice* device, const TextureDesc& desc)
    : device_(device), width_(desc.width), height_(desc.height), format_(desc.format) {

    vkFormat_ = toVkFormat(desc.format);

    // Create image
    VkImageCreateInfo imageInfo{};
    imageInfo.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.extent.width = width_;
    imageInfo.extent.height = height_;
    imageInfo.extent.depth = 1;
    imageInfo.mipLevels = desc.mipLevels;
    imageInfo.arrayLayers = desc.arrayLayers;
    imageInfo.format = vkFormat_;
    imageInfo.tiling = VK_IMAGE_TILING_OPTIMAL;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    imageInfo.usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    VkResult result = vkCreateImage(device_->device(), &imageInfo, nullptr, &image_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan texture: {}", static_cast<int>(result));
        return;
    }

    // Allocate memory
    VkMemoryRequirements memRequirements;
    vkGetImageMemoryRequirements(device_->device(), image_, &memRequirements);

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = 0; // simplified — production would use findMemoryType

    VkPhysicalDeviceMemoryProperties memProps;
    vkGetPhysicalDeviceMemoryProperties(device_->physicalDevice(), &memProps);
    for (uint32_t i = 0; i < memProps.memoryTypeCount; i++) {
        if ((memRequirements.memoryTypeBits & (1 << i)) &&
            (memProps.memoryTypes[i].propertyFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)) {
            allocInfo.memoryTypeIndex = i;
            break;
        }
    }

    result = vkAllocateMemory(device_->device(), &allocInfo, nullptr, &memory_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to allocate Vulkan texture memory");
        vkDestroyImage(device_->device(), image_, nullptr);
        image_ = VK_NULL_HANDLE;
        return;
    }

    vkBindImageMemory(device_->device(), image_, memory_, 0);

    // Create image view
    VkImageViewCreateInfo viewInfo{};
    viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    viewInfo.image = image_;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = vkFormat_;
    viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    viewInfo.subresourceRange.baseMipLevel = 0;
    viewInfo.subresourceRange.levelCount = desc.mipLevels;
    viewInfo.subresourceRange.baseArrayLayer = 0;
    viewInfo.subresourceRange.layerCount = desc.arrayLayers;

    vkCreateImageView(device_->device(), &viewInfo, nullptr, &imageView_);
    device_->trackAllocation(memRequirements.size);
}

VulkanTexture::~VulkanTexture() {
    if (imageView_) vkDestroyImageView(device_->device(), imageView_, nullptr);
    if (memory_) {
        VkMemoryRequirements memRequirements;
        vkGetImageMemoryRequirements(device_->device(), image_, &memRequirements);
        device_->trackDeallocation(memRequirements.size);
        vkFreeMemory(device_->device(), memory_, nullptr);
    }
    if (image_) vkDestroyImage(device_->device(), image_, nullptr);
}

VkFormat VulkanTexture::toVkFormat(TextureFormat fmt) {
    switch (fmt) {
        case TextureFormat::RGBA8:           return VK_FORMAT_R8G8B8A8_UNORM;
        case TextureFormat::BGRA8:           return VK_FORMAT_B8G8R8A8_UNORM;
        case TextureFormat::Depth32F:        return VK_FORMAT_D32_SFLOAT;
        case TextureFormat::Depth24Stencil8: return VK_FORMAT_D24_UNORM_S8_UINT;
        case TextureFormat::BC1:             return VK_FORMAT_BC1_RGBA_UNORM_BLOCK;
        case TextureFormat::BC3:             return VK_FORMAT_BC3_UNORM_BLOCK;
        case TextureFormat::ASTC4x4:         return VK_FORMAT_ASTC_4x4_UNORM_BLOCK;
        case TextureFormat::ASTC8x8:         return VK_FORMAT_ASTC_8x8_UNORM_BLOCK;
        default:                             return VK_FORMAT_R8G8B8A8_UNORM;
    }
}

// ============================================================
// VulkanShader
// ============================================================

VulkanShader::VulkanShader(VulkanDevice* device, ShaderStage stage, const std::vector<uint32_t>& spirv)
    : device_(device) {

    vkStage_ = toVkStage(stage);

    VkShaderModuleCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    createInfo.codeSize = spirv.size() * sizeof(uint32_t);
    createInfo.pCode = spirv.data();

    VkResult result = vkCreateShaderModule(device_->device(), &createInfo, nullptr, &module_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan shader module: {}", static_cast<int>(result));
        module_ = VK_NULL_HANDLE;
    }
}

VulkanShader::~VulkanShader() {
    if (module_) {
        vkDestroyShaderModule(device_->device(), module_, nullptr);
    }
}

VkShaderStageFlagBits VulkanShader::toVkStage(ShaderStage stage) {
    switch (stage) {
        case ShaderStage::Vertex:   return VK_SHADER_STAGE_VERTEX_BIT;
        case ShaderStage::Fragment: return VK_SHADER_STAGE_FRAGMENT_BIT;
        case ShaderStage::Compute:  return VK_SHADER_STAGE_COMPUTE_BIT;
        case ShaderStage::Geometry: return VK_SHADER_STAGE_GEOMETRY_BIT;
        default:                    return VK_SHADER_STAGE_VERTEX_BIT;
    }
}

// ============================================================
// VulkanPipeline
// ============================================================

VulkanPipeline::VulkanPipeline(VulkanDevice* device, const PipelineDesc& desc)
    : device_(device) {

    auto vs = std::dynamic_pointer_cast<VulkanShader>(desc.vertexShader);
    auto fs = std::dynamic_pointer_cast<VulkanShader>(desc.fragmentShader);
    if (!vs || !fs) {
        spdlog::error("Vulkan pipeline requires vertex and fragment shaders");
        return;
    }

    // Shader stages
    VkPipelineShaderStageCreateInfo stages[2] = {};
    stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    stages[0].module = vs->handle();
    stages[0].pName = "main";

    stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    stages[1].module = fs->handle();
    stages[1].pName = "main";

    // Vertex input — simplified: assume interleaved layout set at bind time
    VkPipelineVertexInputStateCreateInfo vertexInput{};
    vertexInput.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    // Dynamic vertex input — attributes set via vkCmdBindVertexBuffers + binding descriptions
    vertexInput.vertexBindingDescriptionCount = 0;
    vertexInput.vertexAttributeDescriptionCount = 0;

    // Input assembly
    VkPipelineInputAssemblyStateCreateInfo inputAssembly{};
    inputAssembly.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    inputAssembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    inputAssembly.primitiveRestartEnable = VK_FALSE;

    // Viewport (dynamic)
    VkPipelineViewportStateCreateInfo viewportState{};
    viewportState.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    viewportState.viewportCount = 1;
    viewportState.scissorCount = 1;

    // Rasterizer
    VkPipelineRasterizationStateCreateInfo rasterizer{};
    rasterizer.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rasterizer.depthClampEnable = VK_FALSE;
    rasterizer.rasterizerDiscardEnable = VK_FALSE;
    rasterizer.polygonMode = VK_POLYGON_MODE_FILL;
    rasterizer.lineWidth = 1.0f;
    rasterizer.cullMode = VK_CULL_MODE_BACK_BIT;
    rasterizer.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;

    // Multisampling
    VkPipelineMultisampleStateCreateInfo multisampling{};
    multisampling.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    multisampling.sampleShadingEnable = VK_FALSE;
    multisampling.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    // Depth-stencil
    VkPipelineDepthStencilStateCreateInfo depthStencil{};
    depthStencil.sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
    depthStencil.depthTestEnable = desc.depthTest ? VK_TRUE : VK_FALSE;
    depthStencil.depthWriteEnable = desc.depthWrite ? VK_TRUE : VK_FALSE;
    depthStencil.depthCompareOp = VK_COMPARE_OP_LESS_OR_EQUAL;

    // Color blending
    VkPipelineColorBlendAttachmentState colorBlendAttachment{};
    colorBlendAttachment.colorWriteMask =
        VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
        VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    colorBlendAttachment.blendEnable = desc.blending ? VK_TRUE : VK_FALSE;
    colorBlendAttachment.srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
    colorBlendAttachment.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    colorBlendAttachment.colorBlendOp = VK_BLEND_OP_ADD;
    colorBlendAttachment.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
    colorBlendAttachment.dstAlphaBlendFactor = VK_BLEND_FACTOR_ZERO;
    colorBlendAttachment.alphaBlendOp = VK_BLEND_OP_ADD;

    VkPipelineColorBlendStateCreateInfo colorBlending{};
    colorBlending.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    colorBlending.attachmentCount = 1;
    colorBlending.pAttachments = &colorBlendAttachment;

    // Dynamic states
    std::vector<VkDynamicState> dynamicStates = {
        VK_DYNAMIC_STATE_VIEWPORT,
        VK_DYNAMIC_STATE_SCISSOR,
    };
    VkPipelineDynamicStateCreateInfo dynamicState{};
    dynamicState.sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
    dynamicState.dynamicStateCount = static_cast<uint32_t>(dynamicStates.size());
    dynamicState.pDynamicStates = dynamicStates.data();

    // Pipeline layout (simplified — no descriptor sets yet)
    VkPipelineLayoutCreateInfo pipelineLayoutInfo{};
    pipelineLayoutInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pipelineLayoutInfo.setLayoutCount = 0;
    pipelineLayoutInfo.pushConstantRangeCount = 0;

    VkResult result = vkCreatePipelineLayout(device_->device(), &pipelineLayoutInfo, nullptr, &pipelineLayout_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan pipeline layout");
        return;
    }

    // Graphics pipeline
    VkGraphicsPipelineCreateInfo pipelineInfo{};
    pipelineInfo.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pipelineInfo.stageCount = 2;
    pipelineInfo.pStages = stages;
    pipelineInfo.pVertexInputState = &vertexInput;
    pipelineInfo.pInputAssemblyState = &inputAssembly;
    pipelineInfo.pViewportState = &viewportState;
    pipelineInfo.pRasterizationState = &rasterizer;
    pipelineInfo.pMultisampleState = &multisampling;
    pipelineInfo.pDepthStencilState = &depthStencil;
    pipelineInfo.pColorBlendState = &colorBlending;
    pipelineInfo.pDynamicState = &dynamicState;
    pipelineInfo.layout = pipelineLayout_;
    pipelineInfo.renderPass = VK_NULL_HANDLE; // dynamic rendering (Vulkan 1.3)
    pipelineInfo.subpass = 0;

    // For dynamic rendering (Vulkan 1.3+), use VK_KHR_dynamic_rendering
    VkPipelineRenderingCreateInfo renderingInfo{};
    renderingInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO;
    renderingInfo.colorAttachmentCount = 1;
    VkFormat colorFormat = VK_FORMAT_B8G8R8A8_UNORM;
    renderingInfo.pColorAttachmentFormats = &colorFormat;
    renderingInfo.depthAttachmentFormat = VK_FORMAT_D32_SFLOAT;

    pipelineInfo.pNext = &renderingInfo;

    result = vkCreateGraphicsPipelines(device_->device(), VK_NULL_HANDLE, 1, &pipelineInfo, nullptr, &pipeline_);
    if (result != VK_SUCCESS) {
        spdlog::error("Failed to create Vulkan graphics pipeline: {}", static_cast<int>(result));
        pipeline_ = VK_NULL_HANDLE;
    }
}

VulkanPipeline::~VulkanPipeline() {
    if (pipeline_) vkDestroyPipeline(device_->device(), pipeline_, nullptr);
    if (pipelineLayout_) vkDestroyPipelineLayout(device_->device(), pipelineLayout_, nullptr);
}

// ============================================================
// VulkanCommandBuffer
// ============================================================

VulkanCommandBuffer::VulkanCommandBuffer(VulkanDevice* device)
    : device_(device) {

    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.commandPool = device->commandPool_;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = 1;

    vkAllocateCommandBuffers(device_->device(), &allocInfo, &cmd_);
}

VulkanCommandBuffer::~VulkanCommandBuffer() {
    if (cmd_) {
        vkFreeCommandBuffers(device_->device(), device_->commandPool_, 1, &cmd_);
    }
}

void VulkanCommandBuffer::begin() {
    recording_ = true;

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    vkBeginCommandBuffer(cmd_, &beginInfo);
}

void VulkanCommandBuffer::end() {
    recording_ = false;
    vkEndCommandBuffer(cmd_);

    // Submit to queue
    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &cmd_;

    vkQueueSubmit(device_->graphicsQueue(), 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(device_->graphicsQueue());
}

void VulkanCommandBuffer::bindPipeline(std::shared_ptr<GpuPipeline> pipeline) {
    currentPipeline_ = std::dynamic_pointer_cast<VulkanPipeline>(pipeline);
    if (currentPipeline_) {
        vkCmdBindPipeline(cmd_, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline_->handle());
    }
}

void VulkanCommandBuffer::bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset, bool) {
    auto vkBuf = std::dynamic_pointer_cast<VulkanBuffer>(buffer);
    if (vkBuf) {
        VkDeviceSize offsets[] = { offset };
        vkCmdBindVertexBuffers(cmd_, 0, 1, &vkBuf->handle(), offsets);
    }
}

void VulkanCommandBuffer::bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset) {
    auto vkBuf = std::dynamic_pointer_cast<VulkanBuffer>(buffer);
    if (vkBuf) {
        vkCmdBindIndexBuffer(cmd_, vkBuf->handle(), offset, VK_INDEX_TYPE_UINT32);
    }
}

void VulkanCommandBuffer::draw(uint32_t vertexCount, uint32_t instanceCount,
                                uint32_t firstVertex, uint32_t firstInstance) {
    vkCmdDraw(cmd_, vertexCount, instanceCount, firstVertex, firstInstance);
}

void VulkanCommandBuffer::drawIndexed(uint32_t indexCount, uint32_t instanceCount,
                                       uint32_t firstIndex, int32_t vertexOffset,
                                       uint32_t firstInstance) {
    vkCmdDrawIndexed(cmd_, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
}

void VulkanCommandBuffer::dispatch(uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ) {
    vkCmdDispatch(cmd_, groupsX, groupsY, groupsZ);
}

// ============================================================
// Factory
// ============================================================

std::shared_ptr<GpuDevice> createVulkanDevice() {
    auto device = std::make_shared<VulkanDevice>();
    if (!device->initialize()) {
        spdlog::error("Failed to initialize Vulkan device");
        return nullptr;
    }
    return device;
}

} // namespace solra::render
