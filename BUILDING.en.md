# Building the Morphe TEST preview bundle

This source tree is self-contained: 13 patch Java files and the complete 81-file owned runtime module. It builds the `.mpp` patch bundle without a Maps APK, account, signing key or device. It does not install anything or contact a server.

Requirements:

- Python 3 and JDK 17 or newer (`java` and `javac` on PATH).
- Android SDK platform 35 and build-tools 36.0.0.
- Morphe Desktop 1.18.1 all-JAR, SHA-256 `1b506ab5f03d16a2f65026d5e0e1910d01fc1e2152f21eaeb44ed2f30856597b`.

Run from this source directory, replacing the three paths with your own:

```sh
python3 build.py \
  --sdk /path/to/android-sdk \
  --cli /path/to/morphe-desktop-1.18.1-all.jar \
  --out /path/to/new-output-directory
```

The output directory must not already exist. The script creates `maps-automotive-for-byd-morphe-preview.4.mpp`, `SHA256SUMS`, `BUILD.json`, intermediate own classes/DEX files and a build log. No original Google application classes or APK are included. The bundle embeds all corresponding patch/runtime Java sources and GPL/upstream notices. ZIP timestamps and entry ordering are fixed; use the same JDK/build-tools for exact binary reproduction.

Applying the bundle is a separate step using the supported original Maps Automotive 26.10.120001.E ARM64 APK (versionCode 1068833407, SHA-256 `d9dc74c9fa0f5bee0911ed1594a19adc0761258d524b5031ed480674d81f5ff9`). Preview 3 sources remain at tag `morphe-preview.3` for 26.09.280001.E. Independently verify its original APK signature before patching. The Google application is not supplied in this source tree. A patched APK must be signed separately with the installer's own consistent key. This builder supplies no signing helper or key and performs no installation.

`known_bev` defaults to false. Set it to true only for a confirmed battery electric vehicle using CCS2 charging stations. It supplies early vehicle identity; it does not choose a vehicle model/capacity, enable the reader or invent charge/range. Profile and reader setup are separate steps.

This builds experimental **Morphe preview 4 / patch revision 54** for Maps Automotive 26.10.120001.E. It carries every preview 3 feature re-pinned to the 26.10 build, the steering-wheel service re-registration after a car restart, CCS1 in the vehicle profile and the DiLink 3 automatic resume. The internal `patch-lab.54-maps2610` name preserves the diagnostics-upload guard; public preview metadata does not enable uploads or the legacy APK updater. Preview 4 has not been retested in Android Manager.
