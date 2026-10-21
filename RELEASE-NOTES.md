# Maps Automotive for BYD — Morphe preview 5

**Release tag: `morphe-preview.5` · patch revision 54 · diagnostic version `patch-lab.54-maps2610` · for Maps Automotive 26.10.120001.E.**

This test update keeps the Maps Automotive 26.10.120001.E base of preview 4. It adds an option to show the instrument-cluster map without opening Maps, wakes the automatic battery reader as soon as the car is switched on, and sends cluster turn hints to the DiLink 3 adapter as well.

**GitHub supplies our own patches and code only.** Apply the `.mpp` to the exact original Maps Automotive APK below, then sign the result locally. The [Telegram ready APK](READY-APK-preview.2.md) is still **preview 2 / build 34**; previews 3–5 are self-build only for now. **Preview 3 stays available for 26.09.280001.E** ([release](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.3)).

## Changes since preview 4

- **Instrument-cluster map without opening Maps (experimental, OFF by default).** Turn on **Instrument cluster → Start cluster map without opening Maps**.
  - When Android has started Maps in the background, the cluster map appears without opening the Maps screen. Maps waits about 5 seconds after its process starts, then shows the cluster map.
  - Switching the car off makes BYD stop Maps. With the **Automatic reader** for BYD battery data enabled, Maps is woken again right after the car is switched on. On the tested Tang the cluster map appeared about 10 seconds after switching the car on, without touching anything.
  - Without the automatic reader, Maps is not woken after the car is switched off and on; open Maps once.
  - The option is for the Tang-style cluster only; it is not shown on DiLink 3 or DiLink 5.1.
  - If a background start fails, Maps does not retry in a loop: it falls back to the usual behaviour and waits until you open it.
- **Automatic battery reader after switching the car on.** The reader now sends a reading as soon as the head unit's screen turns on, instead of waiting up to a minute. Its regular one-minute rhythm is unchanged. No new permissions are used.
- **Cluster turn hints on DiLink 3.** Turn-by-turn hints for the BYD instrument cluster now go to the AMap adapter that is actually installed: `com.byd.amapservice` (DiLink 5.0, as before) or `com.example.amapservice` (DiLink 3). If neither can be confirmed, Maps keeps sending to `com.byd.amapservice`, as before.
- **Diagnostics.** The Support report includes instrument-cluster connection counters: attempts, successes, unexpected closes and their reasons. They contain numbers only, no locations.

Everything else from preview 4 is unchanged.

## Install or update

Use only the clean original **Maps Automotive 26.10.120001.E**, version code **1068833407**, ARM64 (APKMirror variant arm64-v8a + x86_64, nodpi):

```text
d9dc74c9fa0f5bee0911ed1594a19adc0761258d524b5031ed480674d81f5ff9
```

It is listed, for example, on [APKMirror](https://www.apkmirror.com/apk/google-inc/google-maps-android-automotive/google-maps-android-automotive-26-10-120001-e-release/), a third-party site: compare the SHA-256 above before patching. Later 26.10 builds such as 26.10.120003.E are rejected by the compatibility check. The phone Maps APK, an already patched APK and other Maps versions are not supported input. Do not bypass compatibility checks with force or continue-on-error.

- [Add our TEST source to Morphe](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29), or add `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches` in **Sources → + → Remote**. In Morphe Manager choose **Maps Automotive 26.10 TEST → 26.10.120001.E**.
- [Local patch bundle](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.5/maps-automotive-for-byd-morphe-preview.5.mpp) · SHA-256: `50ce9b4eafaec0613d3a40eaf12210afa38382211fd6a64c14f122bc900deb1f`.
- Download [Morphe Manager](https://github.com/MorpheApp/morphe-manager/releases), [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop/releases) and [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) from their official Releases. MicroG-RE 7.2.1 is the tested version.

**Updating from preview 4:** patch the same 26.10.120001.E original with preview 5 and sign with your **same key**. It installs over preview 4 as an update and keeps your Maps data and settings. **From preview 3:** you need the 26.10.120001.E original instead of 26.09; the result still installs as an update with your same key. Changing keys requires a one-time reinstall and loses local Automotive data/settings. Keep the keystore and password backed up.

Morphe can refresh our patch source. It does **not** update installed Maps: patch the supported original again and install the APK signed with your same key. There is no in-app APK OTA.

## Verification and its limits

| Revision | Evidence |
| --- | --- |
| **54, this release (build 54H)** | Tested on one BYD Tang (DiLink 5.0, Android 12), installed as an update keeping app data and accounts. The owner confirmed there: BYD cluster turn hints during a route and their removal at the end of the route; with the new option turned on, the cluster map appeared without opening Maps, about 10 seconds after switching the car on. The car log showed the reader's wake-up reading 0.8 seconds after the screen turned on, and no crashes. The Android verifier accepted every class on the car. Its Google-code changes are identical to preview 4 (only 69 reviewed Google methods change; elsewhere only MicroG string routing), host tests passed, and the public bundle produces an APK payload identical to the car-tested build (3264 entries). |
| **Preview 4 features on 26.10** | Tested on the same Tang earlier: start, map, Search with the EV charge-at-arrival estimate, the instrument-cluster map, pointer offset and cluster zoom during an active route, 3D buildings, interface size, the collapsed Search panel, and steering-wheel zoom after switching the car off and on. These code paths are unchanged in preview 5. |
| **Android 10** | Checked only on an emulator with the preview 4 build. **The preview 5 changes were not run on Android 10.** |
| **DiLink 3** | The AMap adapter choice, the Compose crash fix and the automatic cluster resume have host tests only; **not yet confirmed in a DiLink 3 car**. |
| **DiLink 5.1 and CCS1** | Host tests only. |

The public builder pins **Morphe Desktop 1.18.1**. Preview 5 has not been retested in Android Morphe Manager; Manager 1.35.0 / API32 evidence belongs to preview 1. HUD on DiLink 3, other BYD firmware, DiLink 5.1 vehicles and other Android/tool combinations need further checks. The cluster arrow may stay in its navigation style after a route ends; this is under investigation.

## Optional vehicle integration

Basic Maps use does not require ADB. For live battery/range, enable network ADB, turn on both reader opt-ins under **Instrument cluster → BYD vehicle data**, and authorize the local reader with **Always allow**. Allow Maps startup/background operation in BYD settings. The same automatic reader is what wakes Maps after the car is switched off and on for the new cluster option.

EV planning additionally requires the `known_bev` patch option for a confirmed BEV, plus your own battery capacity in the vehicle profile. It is not PHEV support.

[Installation and feature guide](README.md) · [Migration guide](MIGRATION.md) · [Preview 5 release](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.5)
