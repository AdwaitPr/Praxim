cmake_minimum_required(VERSION 3.22.1)

project("praxim_security_engine")

set(CMAKE_CXX_STANDARD 17)

# Enable LTO and optimizations
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -flto")

# NEON flags (handled primarily via ABI, but explicit definition helps some headers)
add_definitions(-DANDROID_ARM_NEON=ON)

# For 32-bit ARM we add neon FPU flags if needed (usually handled by Android toolchain)
if(${CMAKE_ANDROID_ARCH_ABI} MATCHES "^armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
endif()

add_library(
        praxim_security_engine
        SHARED
        src/main/cpp/praxim_security_engine.cpp
)

find_library(
        log-lib
        log
)

find_library(
        android-lib
        android
)

target_link_libraries(
        praxim_security_engine
        ${log-lib}
        ${android-lib}
        jnigraphics
)
