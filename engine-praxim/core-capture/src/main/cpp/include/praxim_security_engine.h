#ifndef PRAXIM_SECURITY_ENGINE_H
#define PRAXIM_SECURITY_ENGINE_H

#include <jni.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_praxim_core_capture_NativeSecureDetector_nativeIsBlankFrameRgba(
    JNIEnv *env,
    jobject thiz,
    jobject buffer,
    jint width,
    jint height,
    jint rowStrideBytes);

JNIEXPORT jboolean JNICALL
Java_com_praxim_core_capture_NativeSecureDetector_nativeIsBlankFrameYuv(
    JNIEnv *env,
    jobject thiz,
    jobject buffer,
    jint width,
    jint height,
    jint rowStrideBytes,
    jint pixelStrideBytes);

#ifdef __cplusplus
}
#endif

#endif // PRAXIM_SECURITY_ENGINE_H
