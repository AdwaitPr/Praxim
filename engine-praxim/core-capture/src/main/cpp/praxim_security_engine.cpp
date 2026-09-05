#include "include/praxim_security_engine.h"
#include <android/log.h>
#include <arm_neon.h>

#define LOG_TAG "PraximSecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const uint8_t NOISE_THRESHOLD = 3;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_praxim_core_capture_NativeSecureDetector_nativeIsBlankFrameRgba(
    JNIEnv *env,
    jobject thiz,
    jobject buffer,
    jint width,
    jint height,
    jint rowStrideBytes) {

    if (!buffer) return JNI_TRUE;

    uint8_t *data = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
    if (!data) {
        LOGE("Failed to get direct buffer address for RGBA.");
        return JNI_TRUE;
    }

    const int rowStep = 32;
    const int colStep = 16;

    // We only process complete 16-pixel blocks (64 bytes)
    int vecCols = (width / colStep) * colStep;

    uint8x16_t threshold_vec = vdupq_n_u8(NOISE_THRESHOLD);

    for (int y = 0; y < height; y += rowStep) {
        const uint8_t *rowPtr = data + (y * rowStrideBytes);

        for (int x = 0; x < vecCols; x += colStep) {
            uint8x16x4_t rgba = vld4q_u8(rowPtr + (x * 4));

            uint8x16_t max_rg = vmaxq_u8(rgba.val[0], rgba.val[1]);
            uint8x16_t max_rgb = vmaxq_u8(max_rg, rgba.val[2]);

            // Compare max(R,G,B) against threshold
            uint8x16_t mask = vcgtq_u8(max_rgb, threshold_vec);

            // Extract and reduce to see if any pixel exceeds the threshold
#if defined(__aarch64__)
            uint8_t max_val = vmaxvq_u8(mask);
            if (max_val > 0) {
                return JNI_FALSE; // Not blank
            }
#else
            uint8x8_t mask_half = vorr_u8(vget_low_u8(mask), vget_high_u8(mask));
            uint8x8_t max1 = vpmax_u8(mask_half, mask_half);
            uint8x8_t max2 = vpmax_u8(max1, max1);
            uint8x8_t max3 = vpmax_u8(max2, max2);
            if (vget_lane_u8(max3, 0) > 0) {
                return JNI_FALSE;
            }
#endif
        }

        // Boundary loop for trailing pixels
        for (int x = vecCols; x < width; x++) {
            const uint8_t *pixel = rowPtr + (x * 4);
            uint8_t r = pixel[0];
            uint8_t g = pixel[1];
            uint8_t b = pixel[2];
            uint8_t max_rgb = (r > g) ? (r > b ? r : b) : (g > b ? g : b);
            if (max_rgb > NOISE_THRESHOLD) {
                return JNI_FALSE;
            }
        }
    }

    return JNI_TRUE; // Blank frame
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_praxim_core_capture_NativeSecureDetector_nativeIsBlankFrameYuv(
    JNIEnv *env,
    jobject thiz,
    jobject buffer,
    jint width,
    jint height,
    jint rowStrideBytes,
    jint pixelStrideBytes) {

    if (!buffer) return JNI_TRUE;

    uint8_t *data = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
    if (!data) {
        LOGE("Failed to get direct buffer address for YUV.");
        return JNI_TRUE;
    }

    const int rowStep = 32;
    const int colStep = 16;

    uint8x16_t threshold_vec = vdupq_n_u8(NOISE_THRESHOLD);

    if (pixelStrideBytes == 1) {
        int vecCols = (width / colStep) * colStep;

        for (int y = 0; y < height; y += rowStep) {
            const uint8_t *rowPtr = data + (y * rowStrideBytes);

            for (int x = 0; x < vecCols; x += colStep) {
                uint8x16_t y_vals = vld1q_u8(rowPtr + x);

                uint8x16_t mask = vcgtq_u8(y_vals, threshold_vec);

#if defined(__aarch64__)
                uint8_t max_val = vmaxvq_u8(mask);
                if (max_val > 0) {
                    return JNI_FALSE;
                }
#else
                uint8x8_t mask_half = vorr_u8(vget_low_u8(mask), vget_high_u8(mask));
                uint8x8_t max1 = vpmax_u8(mask_half, mask_half);
                uint8x8_t max2 = vpmax_u8(max1, max1);
                uint8x8_t max3 = vpmax_u8(max2, max2);
                if (vget_lane_u8(max3, 0) > 0) {
                    return JNI_FALSE;
                }
#endif
            }

            // Boundary loop
            for (int x = vecCols; x < width; x++) {
                if (rowPtr[x] > NOISE_THRESHOLD) {
                    return JNI_FALSE;
                }
            }
        }
    } else {
        // Fallback for strided Y plane
        for (int y = 0; y < height; y += rowStep) {
            const uint8_t *rowPtr = data + (y * rowStrideBytes);
            for (int x = 0; x < width; x += colStep) { // Use colStep to skip pixels if needed, or scan all
                if (rowPtr[x * pixelStrideBytes] > NOISE_THRESHOLD) {
                    return JNI_FALSE;
                }
            }
        }
    }

    return JNI_TRUE;
}
