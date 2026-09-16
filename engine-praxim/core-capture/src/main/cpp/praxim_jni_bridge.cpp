#include <jni.h>
#include <android/hardware_buffer_jni.h>
#include "praxim_security_engine.hpp"
#include "praxim_signal_guard.hpp"

using namespace praxim::capture;
using namespace praxim::resilience;

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)vm;
    (void)reserved;
    SignalGuard::InitializeGlobalHandlers();
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM* vm, void* reserved) {
    (void)vm;
    (void)reserved;
    SignalGuard::TearDownGlobalHandlers();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_praxim_engine_capture_NativeCaptureCore_nativeValidateAndAuditBuffer(
        JNIEnv* env, jobject thiz, jobject hardwareBufferObj, jboolean probePixels, jintArray outMetadata) {
    (void)thiz;

    if (!hardwareBufferObj) {
        return (static_cast<jlong>(static_cast<int32_t>(SecurityStatus::InvalidHardwareBufferRef)) << 32);
    }

    AHardwareBuffer* buffer = AHardwareBuffer_fromHardwareBuffer(env, hardwareBufferObj);
    if (!buffer) {
        return (static_cast<jlong>(static_cast<int32_t>(SecurityStatus::InvalidHardwareBufferRef)) << 32);
    }

    AuditResult result = SecurityEngine::ValidateAndAuditBuffer(buffer, probePixels);

    if (outMetadata && env->GetArrayLength(outMetadata) >= 4) {
        jint metadata[4] = {
            static_cast<jint>(result.metadata.width),
            static_cast<jint>(result.metadata.height),
            static_cast<jint>(result.metadata.stride),
            static_cast<jint>(result.metadata.format)
        };
        env->SetIntArrayRegion(outMetadata, 0, 4, metadata);
    }

    jlong packedResult = (static_cast<jlong>(static_cast<int32_t>(result.status)) << 32) | (static_cast<uint32_t>(result.latencyUs) & 0xFFFFFFFFLL);
    return packedResult;
}

extern "C" JNIEXPORT void JNICALL
Java_com_praxim_engine_capture_NativeCaptureCore_nativeResetCircuitBreaker(
        JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;
    SecurityEngine::ResetCircuitBreaker();
}
