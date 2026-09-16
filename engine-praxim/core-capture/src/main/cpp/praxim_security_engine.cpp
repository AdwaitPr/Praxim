#include "praxim_security_engine.hpp"
#include "praxim_signal_guard.hpp"
#include <time.h>
#include <atomic>
#include <mutex>
#include <limits>

namespace praxim {
namespace capture {

namespace {
    enum class CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    };

    std::mutex g_cbMutex;
    CircuitBreakerState g_cbState = CircuitBreakerState::CLOSED;
    int g_consecutiveFailures = 0;
    time_t g_lastTripTime = 0;

    const int MAX_FAILURES = 3;
    const time_t COOLDOWN_SECONDS = 10;
    const long TIMEOUT_NS = 2000000; // 2.0ms

    bool CheckCircuitBreaker() {
        std::lock_guard<std::mutex> lock(g_cbMutex);
        if (g_cbState == CircuitBreakerState::OPEN) {
            time_t now = time(nullptr);
            if (now - g_lastTripTime >= COOLDOWN_SECONDS) {
                g_cbState = CircuitBreakerState::HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }

    void RecordSuccess() {
        std::lock_guard<std::mutex> lock(g_cbMutex);
        if (g_cbState == CircuitBreakerState::HALF_OPEN) {
            g_cbState = CircuitBreakerState::CLOSED;
            g_consecutiveFailures = 0;
        } else if (g_cbState == CircuitBreakerState::CLOSED) {
            g_consecutiveFailures = 0;
        }
    }

    void RecordFailure() {
        std::lock_guard<std::mutex> lock(g_cbMutex);
        if (g_cbState == CircuitBreakerState::HALF_OPEN) {
            g_cbState = CircuitBreakerState::OPEN;
            g_lastTripTime = time(nullptr);
        } else if (g_cbState == CircuitBreakerState::CLOSED) {
            g_consecutiveFailures++;
            if (g_consecutiveFailures >= MAX_FAILURES) {
                g_cbState = CircuitBreakerState::OPEN;
                g_lastTripTime = time(nullptr);
            }
        }
    }
}

void SecurityEngine::ResetCircuitBreaker() {
    std::lock_guard<std::mutex> lock(g_cbMutex);
    g_cbState = CircuitBreakerState::CLOSED;
    g_consecutiveFailures = 0;
    g_lastTripTime = 0;
}

AuditResult SecurityEngine::ValidateAndAuditBuffer(AHardwareBuffer* buffer, bool probePixels) {
    AuditResult result;
    result.status = SecurityStatus::UnknownFailure;
    result.latencyUs = 0;
    result.metadata = {0, 0, 0, 0};

    if (!buffer) {
        result.status = SecurityStatus::InvalidHardwareBufferRef;
        return result;
    }

    if (!CheckCircuitBreaker()) {
        result.status = SecurityStatus::CircuitBreakerTripped;
        return result;
    }

    struct timespec start_time, end_time;
    clock_gettime(CLOCK_MONOTONIC_RAW, &start_time);

    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(buffer, &desc);

    result.metadata.width = desc.width;
    result.metadata.height = desc.height;
    result.metadata.stride = desc.stride;
    result.metadata.format = desc.format;

    if ((desc.usage & AHARDWAREBUFFER_USAGE_PROTECTED_CONTENT) != 0) {
        RecordFailure();
        result.status = SecurityStatus::ProtectedSurfaceViolation;
        return result;
    }

    if ((desc.usage & AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN) != AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN) {
        RecordFailure();
        result.status = SecurityStatus::MissingCpuReadPermission;
        return result;
    }

    if (desc.format != AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM &&
        desc.format != AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM) {
        RecordFailure();
        result.status = SecurityStatus::UnsupportedPixelFormat;
        return result;
    }

    if (desc.width == 0 || desc.height == 0 || desc.stride < desc.width) {
        RecordFailure();
        result.status = SecurityStatus::InvalidGeometryOrStride;
        return result;
    }

    // Check for potential integer overflow when calculating size
    uint64_t max_bytes = static_cast<uint64_t>(desc.height - 1) * desc.stride * 4 + static_cast<uint64_t>(desc.width - 1) * 4 + 4;
    if (max_bytes > std::numeric_limits<size_t>::max()) {
        RecordFailure();
        result.status = SecurityStatus::InvalidGeometryOrStride;
        return result;
    }

    if (probePixels) {
        void* virtualAddress = nullptr;
        int lockResult = AHardwareBuffer_lock(buffer, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, nullptr, &virtualAddress);

        if (lockResult != 0 || !virtualAddress) {
            RecordFailure();
            result.status = SecurityStatus::NativeLockFailed;
            return result;
        }

        auto guardResult = praxim::resilience::SignalGuard::ExecuteGuarded([&]() {
            volatile uint8_t* pixels = static_cast<volatile uint8_t*>(virtualAddress);

            // Probe first pixel
            volatile uint8_t first_pixel = pixels[0];
            (void)first_pixel;

            // Probe last scanline boundary pixel
            size_t last_pixel_offset = ((desc.height - 1) * desc.stride * 4) + ((desc.width - 1) * 4);
            volatile uint8_t last_pixel = pixels[last_pixel_offset];
            (void)last_pixel;
        });

        AHardwareBuffer_unlock(buffer, nullptr);

        if (!guardResult.success) {
            RecordFailure();
            result.status = SecurityStatus::PosixSignalTrapped;
            return result;
        }

        if (guardResult.latencyNs > TIMEOUT_NS) {
            RecordFailure();
            result.status = SecurityStatus::ExecutionTimeout;
            return result;
        }
    }

    clock_gettime(CLOCK_MONOTONIC_RAW, &end_time);
    long latencyNs = (end_time.tv_sec - start_time.tv_sec) * 1000000000L + (end_time.tv_nsec - start_time.tv_nsec);
    result.latencyUs = static_cast<uint32_t>(latencyNs / 1000);

    RecordSuccess();
    result.status = SecurityStatus::Permitted;
    return result;
}

} // namespace capture
} // namespace praxim
