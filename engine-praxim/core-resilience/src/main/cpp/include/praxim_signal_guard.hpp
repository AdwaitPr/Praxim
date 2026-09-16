#pragma once

#include <functional>

namespace praxim {
namespace resilience {

struct GuardResult {
    bool success;
    long latencyNs;
};

class SignalGuard {
public:
    static void InitializeGlobalHandlers();
    static void TearDownGlobalHandlers();
    static GuardResult ExecuteGuarded(const std::function<void()>& action);
};

} // namespace resilience
} // namespace praxim
