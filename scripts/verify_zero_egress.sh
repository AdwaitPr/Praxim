#!/usr/bin/env bash
set -euo pipefail

echo "======================================================================"
echo "🛡️  STARTING ZERO-EGRESS MANIFEST AUDIT..."
echo "======================================================================"

MANIFEST_PATH=$(find app/build/intermediates -name "AndroidManifest.xml" | grep "processDebugMainManifest" | head -n 1 || true)

if [[ -z "${MANIFEST_PATH}" || ! -f "${MANIFEST_PATH}" ]]; then
    echo "⚠️ Merged manifest not found yet. Running manifest merger..."
    ./gradlew :app:processDebugMainManifest
    MANIFEST_PATH=$(find app/build/intermediates -name "AndroidManifest.xml" | grep "processDebugMainManifest" | head -n 1)
fi

echo "Auditing: ${MANIFEST_PATH}"

FORBIDDEN_PERMISSIONS=(
    "android.permission.INTERNET"
    "android.permission.ACCESS_NETWORK_STATE"
    "android.permission.ACCESS_WIFI_STATE"
    "android.permission.CHANGE_WIFI_STATE"
    "android.permission.CHANGE_NETWORK_STATE"
)

VIOLATIONS=0
for PERM in "${FORBIDDEN_PERMISSIONS[@]}"; do
    if grep -q "${PERM}" "${MANIFEST_PATH}"; then
        echo "🚨 CRITICAL VIOLATION: Forbidden permission detected -> ${PERM}"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done

if [[ ${VIOLATIONS} -gt 0 ]]; then
    echo "❌ ZERO-EGRESS AUDIT FAILED: ${VIOLATIONS} forbidden permissions detected."
    exit 1
else
    echo "✅ ZERO-EGRESS AUDIT PASSED: 100% on-device architecture verified."
    exit 0
fi
