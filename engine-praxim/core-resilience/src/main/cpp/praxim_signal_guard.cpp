#include "praxim_signal_guard.hpp"
#include <signal.h>
#include <setjmp.h>
#include <time.h>
#include <atomic>
#include <android/log.h>

#define LOG_TAG "PraximSignalGuard"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace praxim {
namespace resilience {

namespace {
    thread_local sigjmp_buf g_jmpBuf;
    thread_local volatile sig_atomic_t g_inGuardedSection = 0;

    struct sigaction g_oldSegvAction;
    struct sigaction g_oldBusAction;
    std::atomic<bool> g_initialized{false};

    void SignalHandler(int signum, siginfo_t* info, void* context) {
        if (g_inGuardedSection) {
            // Unwind back to the setjmp point, passing 1 to indicate signal trapped
            siglongjmp(g_jmpBuf, 1);
        } else {
            // Forward to ART / previous handler if outside guarded section
            struct sigaction* oldAct = nullptr;
            if (signum == SIGSEGV) oldAct = &g_oldSegvAction;
            else if (signum == SIGBUS) oldAct = &g_oldBusAction;

            if (oldAct && oldAct->sa_sigaction) {
                oldAct->sa_sigaction(signum, info, context);
            } else if (oldAct && oldAct->sa_handler != SIG_DFL && oldAct->sa_handler != SIG_IGN) {
                oldAct->sa_handler(signum);
            } else {
                // Default action: restore default and re-raise
                struct sigaction defaultAct{};
                defaultAct.sa_handler = SIG_DFL;
                sigemptyset(&defaultAct.sa_mask);
                defaultAct.sa_flags = 0;
                sigaction(signum, &defaultAct, nullptr);
                raise(signum);
            }
        }
    }
} // anonymous namespace

void SignalGuard::InitializeGlobalHandlers() {
    if (g_initialized.exchange(true)) return;

    struct sigaction sa{};
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
    sa.sa_sigaction = SignalHandler;
    sigemptyset(&sa.sa_mask);

    // Add both signals to mask during handler execution
    sigaddset(&sa.sa_mask, SIGSEGV);
    sigaddset(&sa.sa_mask, SIGBUS);

    if (sigaction(SIGSEGV, &sa, &g_oldSegvAction) != 0) {
        LOGE("Failed to set SIGSEGV handler");
    }
    if (sigaction(SIGBUS, &sa, &g_oldBusAction) != 0) {
        LOGE("Failed to set SIGBUS handler");
    }
}

void SignalGuard::TearDownGlobalHandlers() {
    if (!g_initialized.exchange(false)) return;

    sigaction(SIGSEGV, &g_oldSegvAction, nullptr);
    sigaction(SIGBUS, &g_oldBusAction, nullptr);
}

GuardResult SignalGuard::ExecuteGuarded(const std::function<void()>& action) {
    GuardResult result;
    result.success = false;
    result.latencyNs = 0;

    struct timespec start, end;
    clock_gettime(CLOCK_MONOTONIC_RAW, &start);

    g_inGuardedSection = 1;
    if (sigsetjmp(g_jmpBuf, 1) == 0) {
        action();
        result.success = true;
    }
    g_inGuardedSection = 0;

    clock_gettime(CLOCK_MONOTONIC_RAW, &end);
    result.latencyNs = (end.tv_sec - start.tv_sec) * 1000000000L + (end.tv_nsec - start.tv_nsec);

    return result;
}

} // namespace resilience
} // namespace praxim
