#pragma once

#include <android/hardware_buffer.h>
#include <stdint.h>

namespace praxim {
namespace capture {

enum class SecurityStatus : int32_t {
    Permitted = 0,
    ProtectedSurfaceViolation = 1,
    MissingCpuReadPermission = 2,
    UnsupportedPixelFormat = 3,
    InvalidGeometryOrStride = 4,
    NativeLockFailed = 5,
    ExecutionTimeout = 6,
    CircuitBreakerTripped = 7,
    PosixSignalTrapped = 8,
    InvalidHardwareBufferRef = 9,
    UnknownFailure = 10
};

struct AuditMetadata {
    uint32_t width;
    uint32_t height;
    uint32_t stride;
    uint32_t format;
};

struct AuditResult {
    SecurityStatus status;
    uint32_t latencyUs;
    AuditMetadata metadata;
};

class SecurityEngine {
public:
    static AuditResult ValidateAndAuditBuffer(AHardwareBuffer* buffer, bool probePixels = true);
    static void ResetCircuitBreaker();
};

} // namespace capture
} // namespace praxim
