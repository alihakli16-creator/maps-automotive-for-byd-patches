# Building the Morphe TEST preview bundle

This source tree is self-contained:7 patch Java files and the complete62-file owned runtime module. It builds the `.mpp` patch bundle without a Maps APK, account, signing key or device. It does not install anything or contact a server.

Requirements:

- Python3 and JDK17 or newer (`java` and `javac` on PATH).
- Android SDK platform35 and build-tools36.0.0.
- Morphe Desktop1.18.1 all-JAR, SHA-256 `1b506ab5f03d16a2f65026d5e0e1910d01fc1e2152f21eaeb44ed2f30856597b`.

Run from this source directory, replacing the three paths with your own:

```sh
python3 build.py \
  --sdk /path/to/android-sdk \
  --cli /path/to/morphe-desktop-1.18.1-all.jar \
  --out /path/to/new-output-directory
```

The output directory must not already exist. The script creates `maps-automotive-for-byd-morphe-preview.1.mpp`, `SHA256SUMS`, `BUILD.json`, intermediate own classes/DEX files and a build log. No original Google application classes or APK are included. The bundle embeds all corresponding patch/runtime Java sources and GPL/upstream notices. ZIP timestamps and entry ordering are fixed; use the same JDK/build-tools for exact binary reproduction.

Applying the bundle is a separate step using the supported original Maps Automotive26.09.280001.E ARM64 APK (versionCode1068809778, SHA-256 `a9ecc4c18fac089c3c55fd20bed65233b7ea4db029da9b824305bf0b4b5716c9`). Independently verify its original APK signature before patching. The Google application is not supplied in this source tree. A patched APK must be signed separately with the installer's own consistent key. This builder supplies no signing helper or key and performs no installation.

`known_bev` defaults to false. Set it to true only for a confirmed battery electric vehicle using CCS2 charging stations. It supplies early vehicle identity; it does not choose a vehicle model/capacity, enable the reader or invent charge/range. Profile and reader setup are separate steps.

This is an experimental Desktop preview. Android Morphe Manager support must be stated according to its separate verification results. The internal `patch-lab.26` runtime name is intentional: it preserves the diagnostics-upload guard from the tested Morphe25 runtime. Public preview metadata does not enable uploads or the legacy APK updater.
