# Maps Automotive for BYD — Morphe preview 4

**Release tag: `morphe-preview.4` · patch revision 54 · diagnostic version `patch-lab.54-maps2610` · for Maps Automotive 26.10.120001.E.**

This test update moves every preview 3 feature to the newer original **Maps Automotive 26.10.120001.E**. It also keeps steering-wheel zoom working after you switch the car off and on, adds CCS1 to the vehicle profile, and lets the DiLink 3 cluster map resume by itself after a clean session.

**GitHub supplies our own patches and code only.** Apply the `.mpp` to the exact original Maps Automotive APK below, then sign the result locally. The [Telegram ready APK](READY-APK-preview.2.md) is still **preview 2 / build 34**; preview 3 and preview 4 are self-build only for now. **Preview 3 stays available for 26.09.280001.E** ([release](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.3)).

## Changes since preview 3

- **Maps Automotive 26.10.120001.E.** Google renamed almost every internal class in this build. All our hooks were re-pinned to the new names and checked against the original: instrument cluster and HUD, steering-wheel cluster zoom, navigation pointer offset and zoom, vehicle profile and EV route planning, interface size, 3D buildings, Search alignment, Android 10 compatibility, the DiLink 3 cluster-map test and the DiLink 5.1 theme profile. Use exactly **26.10.120001.E** (version code 1068833407); later 26.10 builds such as 26.10.120003.E are rejected by the compatibility check.
- **Steering-wheel zoom after switching the car off and on.** Switching the car off makes BYD stop Maps. Android then kept our key-only accessibility service listed but did not connect it again, so the custom button rotated the screen until you turned **Steering-wheel zoom** off and on. Maps now re-registers only its own service entry once, about 10 seconds after start, through the already approved ADB key. There is no new prompt, other accessibility services are kept, and nothing happens if the service is already connected or the feature is off. Preview 3 has the old behaviour.
- **CCS1 in the vehicle profile.** **BYD vehicle data → Vehicle profile** now offers CCS1 for BEV and PHEV profiles, after CCS2. EV route planning remains **BEV-only**.
- **DiLink 3 cluster map (experimental, Android 10 only).** After the car is switched off and on, the projection resumes once by itself, but only if the previous projection ran for at least 30 seconds without an error. In every other case it still waits for **Authorize or retry**, which protects against crash loops after an unclean session.

Everything else from preview 3 is unchanged. The experimental side panel shown to some testers is not part of this release.

## Install or update

Use only the clean original **Maps Automotive 26.10.120001.E**, version code **1068833407**, ARM64 (APKMirror variant arm64-v8a + x86_64, nodpi):

```text
d9dc74c9fa0f5bee0911ed1594a19adc0761258d524b5031ed480674d81f5ff9
```

It is listed, for example, on [APKMirror](https://www.apkmirror.com/apk/google-inc/google-maps-android-automotive/google-maps-android-automotive-26-10-120001-e-release/), a third-party site: compare the SHA-256 above before patching. The phone Maps APK, an already patched APK and other Maps versions are not supported input. Do not bypass compatibility checks with force or continue-on-error.

- [Add our TEST source to Morphe](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29), or add `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches` in **Sources → + → Remote**. In Morphe Manager choose **Maps Automotive 26.10 TEST → 26.10.120001.E**.
- [Local patch bundle](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.4/maps-automotive-for-byd-morphe-preview.4.mpp) · SHA-256: `599cf4eae88adbe463eecdbe96f3f504dfaaa1f4ec6cc0fcf38aa5c83a137d2f`.
- Download [Morphe Manager](https://github.com/MorpheApp/morphe-manager/releases), [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop/releases) and [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) from their official Releases. MicroG-RE 7.2.1 is the tested version.

**Updating from preview 3:** patch the 26.10.120001.E original and sign with your **same key**. The package and version code are unchanged, so the result installs over preview 3 as an update and keeps your Maps data and settings. Changing keys requires a one-time reinstall and loses local Automotive data/settings. Keep the keystore and password backed up.

Morphe can refresh our patch source. It does **not** update installed Maps: patch the supported original again and install the APK signed with your same key. There is no in-app APK OTA.

## Verification and its limits

| Revision | Evidence |
| --- | --- |
| **54, this release** | On one BYD Tang (DiLink 5.0, Android 12), installed as an update keeping app data and accounts. The owner checked there: start, map, Search with the EV charge-at-arrival estimate and the instrument-cluster map; during an active route the pointer offset (−10/+10), cluster zoom with the steering wheel without rotating the screen and volume restore by the second press; BYD cluster turn hints that clear at the end of the route; 3D buildings on/off, interface size and the collapsed Search panel; steering-wheel zoom after switching the car off and on without touching the setting. Some of these checks used a later test build of revision 54 that only changes how the AMap adapter package is chosen; its Google-code changes are identical to this release. On an Android 10 (API 29) emulator: installed as an update with data and account kept, every class accepted by the Android 10 verifier, start and map, no crashes in two minutes. Applied with Morphe Desktop 1.18.1, the public bundle produces an APK payload identical to that build (3264 entries). Host tests passed, every runtime lookup was checked against the 26.10 original, and written-DEX checks confirm that only 69 reviewed Google methods change; elsewhere only MicroG string routing. A new register-flow check of every hook also passed. |
| **DiLink 3 (Android 10)** | The Compose crash fix and the new automatic resume have host tests and an emulator start only; **not yet confirmed in a DiLink 3 car**. A tester's photos showed the cluster map on a DiLink 3 hybrid with revision 40. |
| **DiLink 5.1** | Host tests only; **no vehicle test yet**. |
| **CCS1** | Host tests only. |
| **Preview 2/3 features** | Ported unchanged in behaviour; earlier evidence is in the [preview 3 notes](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.3). |

The public builder pins **Morphe Desktop 1.18.1**. Preview 4 has not been retested in Android Morphe Manager; Manager 1.35.0 / API32 evidence belongs to preview 1. HUD on DiLink 3, other BYD firmware, DiLink 5.1 vehicles and other Android/tool combinations need further checks. The cluster arrow may stay in its navigation style after a route ends; this is under investigation.

## Optional vehicle integration

Basic Maps use does not require ADB. For live battery/range, enable network ADB, turn on both reader opt-ins under **Instrument cluster → BYD vehicle data**, and authorize the local reader with **Always allow**. Allow Maps startup/background operation in BYD settings.

EV planning additionally requires the `known_bev` patch option for a confirmed BEV, plus your own battery capacity in the vehicle profile. It is not PHEV support.

[Installation and feature guide](README.md) · [Migration guide](MIGRATION.md) · [Preview 4 release](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.4)
