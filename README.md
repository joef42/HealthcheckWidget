# HealthcheckWidget

An Android home screen widget that displays healthcheck status.

## Building

Builds run inside a Podman container — no JDK or Android SDK needed on the host.

### First-time setup

```bash
# Build the toolchain image (only needed once, or after Dockerfile changes)
podman build --isolation=chroot -t healthcheck-widget-builder .
```

### Build the APK

```bash
podman run --rm --pid=host --workdir /project \
  -v "$(pwd)":/project:z \
  -v gradle-cache:/root/.gradle \
  healthcheck-widget-builder \
  ./gradlew assembleDebug --no-daemon
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Notes

- `--pid=host` is required when building inside a nested container environment (read-only `/proc`).
- `--isolation=chroot` is required for `podman build` in the same environment; it is not a valid flag for `podman run`.
- The `gradle-cache` volume persists the Gradle distribution and Maven dependencies across builds, avoiding ~40 s of re-download overhead after the first run. Clear it with `podman volume rm gradle-cache` if the cache ever needs a reset.
