# Fold Power Lab 0.2.0

Standalone Android 34+ package `org.duofold.powerlab`, based on Fold Observer 0.1.4. Build with `bash tools/fold-power-lab/build.sh` after installing JDK17 and Android SDK36/build-tools35.0.0. CI signs it alongside the Duo alpha with the existing release key. No Internet permission; local ZIP exports use the Android document picker.

See `docs/3.5.9-alpha.1-CHANGELOG.md` for experiment order, bounded overrides, evidence limits and report contents. Disable Duo before an override. The conflict guard rejects a current device state different from the base state; this is not exhaustive detection of every other app's power lease. Do not run multiple override tools simultaneously.

Read-only API inventory is intentionally broader than callable tests. Unsupported signatures, rejected permissions, missing sensors and capture failures are reported rather than interpreted as successful overrides. Per-display ON uses the observed Samsung four-argument token API and explicit token release. It does not use the argument 10000 as a verified timeout; the independent ten-second cleanup controls lifetime. A fifteen-second worker-death fallback releases process-owned tokens if cleanup fails and requires reconnecting; this may prevent the report from finishing.
