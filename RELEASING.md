# Releasing WaveBalance

Releases are published on [sanjaynathwani-blip/wavebalance](https://github.com/sanjaynathwani-blip/wavebalance/releases)
as `WaveBalance.apk`, which the README's download link points to. A release goes out as a
**pre-release** first, is installed on a real device the way a user would install it (no adb),
and only then becomes the latest release.

## 1. Prepare

1. In `app/build.gradle.kts`, bump `versionCode` by one and set `versionName`.
2. Commit as "Version X.Y.Z" and push.

## 2. Build

The release key lives in `~/.config/wavebalance/` (never in the repo). Every release must be
signed with it, or Android refuses to update over earlier installs.

```sh
./gradlew testDebugUnitTest assembleRelease
mkdir -p build/release && cp app/build/outputs/apk/release/app-release.apk build/release/WaveBalance.apk
(cd build/release && sha256sum WaveBalance.apk > SHA256SUMS)
apksigner verify --print-certs build/release/WaveBalance.apk | grep SHA-256
# expected: ea6929bb692cc1825cca3fa82d39bc39ddf222d1ba76087ee3473276faab818d
```

## 3. Publish as a pre-release

```sh
git tag vX.Y.Z && git push origin vX.Y.Z
gh release create vX.Y.Z build/release/WaveBalance.apk build/release/SHA256SUMS \
  -R sanjaynathwani-blip/wavebalance --prerelease --title "WaveBalance X.Y.Z" --notes-file build/release/notes.md
```

## 4. First-install check, on the device, without adb

1. Uninstall WaveBalance (debug builds are "WaveBalance Dev" and can stay).
2. In Chrome on the device, open the pre-release's page, download **WaveBalance.apk**, open it
   and install it. Play Protect's "unknown developer" warning is expected until the developer
   account is verified (see Android developer verification); anything else is new.
3. Open WaveBalance, allow location and nearby devices: the Dashboard and Radar fill in.
4. Run a full speed test: the M-Lab consent appears, and download, upload and bufferbloat show.

## 5. Make it the latest release

```sh
gh release edit vX.Y.Z -R sanjaynathwani-blip/wavebalance --prerelease=false --latest
```

If a step in the check fails, fix it, bump the version again and start over; don't replace a
published APK.
