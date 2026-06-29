#pragma once
// HTTP/3 + QUIC Transport: low-latency reliable streaming for asset chunks
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <optional>

namespace solra::streaming {

// ---- QUIC Configuration ----
struct QuicConfig {
    std::string serverHost;
    uint16_t serverPort = 4433;
    uint32_t maxStreams = 100;
    uint64_t maxStreamData = 16 * 1024 * 1024; // 16 MB per stream
    uint32_t idleTimeoutMs = 30'000;           // 30s
    uint32_t keepAliveIntervalMs = 10'000;      // 10s
    uint32_t initialRttMs = 100;
    bool enable0Rtt = true;                     // 0-RTT resumption
    bool enableDatagrams = true;                 // unreliable datagram extension
    std::string tlsCertFile;                    // PEM cert for mutual TLS
    std::string tlsKeyFile;
    std::string alpn = "h3";                    // HTTP/3 ALPN
};

// ---- HTTP/3 Request ----
struct Http3Request {
    std::string path;
    std::string method = "GET";
    std::vector<std::pair<std::string, std::string>> headers;
    uint64_t rangeStart = 0;    // Range: bytes=start-end
    uint64_t rangeEnd = 0;      // 0 = until EOF
    bool useDatagram = false;   // send via QUIC datagram instead of stream
};

// ---- HTTP/3 Response ----
struct Http3Response {
    int statusCode = 0;
    std::vector<std::pair<std::string, std::string>> headers;
    std::vector<uint8_t> body;
    uint64_t contentLength = 0;
    bool isChunked = false;
};

// ---- Streaming callbacks ----
using DataChunkCallback = std::function<void(const uint8_t* data, size_t size, bool isLast)>;
using ErrorCallback = std::function<void(int errorCode, const std::string& message)>;

// ---- QuicTransport ----
class QuicTransport {
public:
    virtual ~QuicTransport() = default;

    // Lifecycle
    virtual bool connect(const QuicConfig& config) = 0;
    virtual void disconnect() = 0;
    virtual bool isConnected() const = 0;

    // Request-Response (stream)
    virtual std::optional<Http3Response> request(const Http3Request& req,
                                                   uint32_t timeoutMs = 10000) = 0;

    // Streaming download (for asset chunks)
    virtual bool streamDownload(const Http3Request& req,
                                 DataChunkCallback onData,
                                 ErrorCallback onError) = 0;
    virtual void cancelStream(uint64_t streamId) = 0;

    // Unreliable datagrams (for state sync, not asset transfer)
    virtual bool sendDatagram(const uint8_t* data, size_t size) = 0;
    using DatagramCallback = std::function<void(const uint8_t* data, size_t size)>;
    virtual void setDatagramHandler(DatagramCallback handler) = 0;

    // Stats
    struct QuicStats {
        uint64_t bytesSent, bytesReceived;
        uint64_t packetsSent, packetsReceived, packetsLost;
        uint32_t smoothedRttUs;
        uint32_t congestionWindow;
    };
    virtual QuicStats stats() const = 0;
};

// ---- Factory ----
// Uses msquic (Windows/Linux) / Network.framework QUIC (iOS/macOS) / Cronet (Android)
std::unique_ptr<QuicTransport> createQuicTransport();

} // namespace solra::streaming
