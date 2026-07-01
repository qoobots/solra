#pragma once
// Vulkan 1.3 backend implementation of GpuDevice interface
// Supports Windows, Linux, and macOS (via MoltenVK)

#include "gpu_abstraction.hpp"
#include <vulkan/vulkan.h>
#include <vector>
#include <string>
#include <unordered_map>
#include <mutex>

namespace solra::render {

// ============================================================
// VulkanDevice
// ============================================================
class VulkanDevice : public GpuDevice {
public:
    explicit VulkanDevice();
    ~VulkanDevice() override;

    // GpuDevice interface
    std::string name() const override;
    Backend backend() const override { return Backend::Vulkan; }

    std::shared_ptr<GpuBuffer> createBuffer(const BufferDesc& desc, const void* data = nullptr) override;
    std::shared_ptr<GpuTexture> createTexture(const TextureDesc& desc) override;
    std::shared_ptr<GpuShader> createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) override;
    std::shared_ptr<GpuPipeline> createPipeline(const PipelineDesc& desc) override;

    std::shared_ptr<GpuCommandBuffer> createCommandBuffer() override;
    void submit(std::shared_ptr<GpuCommandBuffer> cmd) override;
    void present() override;
    void waitIdle() override;

    uint64_t gpuMemoryUsed() const override;
    uint64_t gpuMemoryBudget() const override;

    // Vulkan-specific
    bool initialize();
    void shutdown();
    bool isInitialized() const { return initialized_; }

    VkDevice device() const { return device_; }
    VkPhysicalDevice physicalDevice() const { return physicalDevice_; }
    VkInstance instance() const { return instance_; }
    uint32_t graphicsQueueFamily() const { return graphicsQueueFamily_; }
    VkQueue graphicsQueue() const { return graphicsQueue_; }

private:
    bool initialized_ = false;

    // Vulkan handles
    VkInstance instance_ = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue graphicsQueue_ = VK_NULL_HANDLE;
    uint32_t graphicsQueueFamily_ = 0;

    // Debug messenger (optional)
    VkDebugUtilsMessengerEXT debugMessenger_ = VK_NULL_HANDLE;

    // Command pool
    VkCommandPool commandPool_ = VK_NULL_HANDLE;

    // Memory tracking
    uint64_t memoryUsed_ = 0;
    uint64_t memoryBudget_ = 4096ULL * 1024 * 1024; // 4GB default
    std::mutex resourceMutex_;

    // Device info
    std::string deviceName_;
    std::string vendorName_;

    // Initialization helpers
    bool createInstance();
    bool pickPhysicalDevice();
    bool createLogicalDevice();
    bool createCommandPool();
    void setupDebugMessenger();
    void destroyDebugMessenger();

    void trackAllocation(uint64_t bytes);
    void trackDeallocation(uint64_t bytes);
};

// ============================================================
// VulkanBuffer
// ============================================================
class VulkanBuffer : public GpuBuffer {
public:
    VulkanBuffer(VulkanDevice* device, const BufferDesc& desc, const void* data);
    ~VulkanBuffer() override;

    void* map() override;
    void unmap() override;
    uint64_t size() const override { return size_; }

    VkBuffer handle() const { return buffer_; }

private:
    VulkanDevice* device_;
    VkBuffer buffer_ = VK_NULL_HANDLE;
    VkDeviceMemory memory_ = VK_NULL_HANDLE;
    uint64_t size_ = 0;
    bool mapped_ = false;
    void* mappedPtr_ = nullptr;

    uint32_t findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties);
};

// ============================================================
// VulkanTexture
// ============================================================
class VulkanTexture : public GpuTexture {
public:
    VulkanTexture(VulkanDevice* device, const TextureDesc& desc);
    ~VulkanTexture() override;

    uint32_t width() const override { return width_; }
    uint32_t height() const override { return height_; }

    VkImage handle() const { return image_; }
    VkImageView view() const { return imageView_; }
    VkFormat format() const { return vkFormat_; }

private:
    VulkanDevice* device_;
    VkImage image_ = VK_NULL_HANDLE;
    VkDeviceMemory memory_ = VK_NULL_HANDLE;
    VkImageView imageView_ = VK_NULL_HANDLE;
    VkFormat vkFormat_ = VK_FORMAT_R8G8B8A8_UNORM;
    uint32_t width_ = 1, height_ = 1;
    TextureFormat format_;

    VkFormat toVkFormat(TextureFormat fmt);
    void transitionLayout(VkImageLayout oldLayout, VkImageLayout newLayout);
};

// ============================================================
// VulkanShader
// ============================================================
class VulkanShader : public GpuShader {
public:
    VulkanShader(VulkanDevice* device, ShaderStage stage, const std::vector<uint32_t>& spirv);
    ~VulkanShader() override;

    VkShaderModule handle() const { return module_; }
    VkShaderStageFlagBits vkStage() const { return vkStage_; }

    static VkShaderStageFlagBits toVkStage(ShaderStage stage);

private:
    VulkanDevice* device_;
    VkShaderModule module_ = VK_NULL_HANDLE;
    VkShaderStageFlagBits vkStage_;
};

// ============================================================
// VulkanPipeline
// ============================================================
class VulkanPipeline : public GpuPipeline {
public:
    VulkanPipeline(VulkanDevice* device, const PipelineDesc& desc);
    ~VulkanPipeline() override;

    VkPipeline handle() const { return pipeline_; }
    VkPipelineLayout layout() const { return pipelineLayout_; }

private:
    VulkanDevice* device_;
    VkPipeline pipeline_ = VK_NULL_HANDLE;
    VkPipelineLayout pipelineLayout_ = VK_NULL_HANDLE;

    VkFormat toVkFormat(CompareOp op);
    VkFormat toVkBlend(BlendFactor f);
};

// ============================================================
// VulkanCommandBuffer
// ============================================================
class VulkanCommandBuffer : public GpuCommandBuffer {
public:
    VulkanCommandBuffer(VulkanDevice* device);
    ~VulkanCommandBuffer() override;

    void begin() override;
    void end() override;
    void bindPipeline(std::shared_ptr<GpuPipeline> pipeline) override;
    void bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0, bool skinned = false) override;
    void bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0) override;
    void draw(uint32_t vertexCount, uint32_t instanceCount = 1,
              uint32_t firstVertex = 0, uint32_t firstInstance = 0) override;
    void drawIndexed(uint32_t indexCount, uint32_t instanceCount = 1,
                     uint32_t firstIndex = 0, int32_t vertexOffset = 0,
                     uint32_t firstInstance = 0) override;
    void dispatch(uint32_t groupsX, uint32_t groupsY = 1, uint32_t groupsZ = 1) override;

private:
    VulkanDevice* device_;
    VkCommandBuffer cmd_ = VK_NULL_HANDLE;
    std::shared_ptr<VulkanPipeline> currentPipeline_;
    bool recording_ = false;
};

// Factory
std::shared_ptr<GpuDevice> createVulkanDevice();

} // namespace solra::render
