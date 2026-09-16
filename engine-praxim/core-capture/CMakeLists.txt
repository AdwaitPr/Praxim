cmake_minimum_required(VERSION 3.22.1)

project("praxim_capture")

add_library(praxim_capture SHARED
        src/main/cpp/praxim_security_engine.cpp
        src/main/cpp/praxim_jni_bridge.cpp)

target_include_directories(praxim_capture PRIVATE
        src/main/cpp/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../core-resilience/src/main/cpp/include)

find_library(log-lib log)
find_library(android-lib android)
find_library(nativewindow-lib nativewindow)

target_link_libraries(praxim_capture
        praxim_resilience
        ${android-lib}
        ${nativewindow-lib}
        ${log-lib})
