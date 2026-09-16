cmake_minimum_required(VERSION 3.22.1)

project("praxim_engine")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden -fvisibility-inlines-hidden -fstack-protector-strong -D_FORTIFY_SOURCE=2 -Wall -Wextra -Werror -fno-exceptions -fno-rtti")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,relro -Wl,-z,now -Wl,--gc-sections -Wl,--exclude-libs,ALL")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-z,relro -Wl,-z,now -Wl,--gc-sections -Wl,--exclude-libs,ALL")

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../../../engine-praxim/core-resilience engine-praxim/core-resilience)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../../../engine-praxim/core-capture engine-praxim/core-capture)
