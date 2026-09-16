cmake_minimum_required(VERSION 3.22.1)

project("praxim_resilience")

add_library(praxim_resilience STATIC src/main/cpp/praxim_signal_guard.cpp)

target_include_directories(praxim_resilience PUBLIC src/main/cpp/include)

find_library(log-lib log)

target_link_libraries(praxim_resilience ${log-lib})
