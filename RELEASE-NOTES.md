# Maps Automotive for BYD — Morphe preview 3

**Release tag: `morphe-preview.3` · patch revision 52 · diagnostic version `patch-lab.52-unified`.**

This test update combines the instrument-cluster work since preview 2 into one patch. It adds steering-wheel zoom for the cluster map and fixes the cluster pointer position and zoom during navigation. It also adds a vehicle profile for BEV/PHEV/HEV, plus experimental support for Android 10 / DiLink 3 and DiLink 5.1 head units.

**GitHub supplies our own patches and code only.** Apply the `.mpp` to the exact original Maps Automotive APK below, then sign the result locally. The [Telegram ready APK](READY-APK-preview.2.md) is still **preview 2 / build 34**; preview 3 is self-build only for now.

## Changes since preview 2

- **Steering-wheel zoom for the cluster map.** Enable **Instrument cluster → Steering-wheel zoom**. Press the custom steering-wheel button, then volume + or − zooms the cluster map; press the custom button again to restore volume. The chosen zoom stays until **Reset cluster zoom** or until the cluster map is recreated. Setup uses ADB on the head unit and a key-only accessibility service, which does not read screen content. Approve the ADB prompt once. Default buttons are for the BYD Tang; other firmware may use different buttons. The feature is OFF by default.
- **Cluster pointer and zoom during navigation.** **Map layout → Pointer vertical offset** now also moves the navigation arrow during active guidance. Cluster zoom also works during navigation without rotating the screen.
- **Vehicle profile.** **BYD vehicle data → Vehicle profile** offers Unknown / BEV / PHEV / HEV, a name, usable battery capacity and the connectors you actually use (CCS2, Type 2 AC, GB/T AC or DC, adapter). After changing type or connector, use Force stop and reopen Maps. EV route planning remains **BEV-only**. PHEV/HEV profiles describe the car, but this bridge supplies no fuel or combined energy model.
- **Android 10 support (experimental).** The patch now accepts Android 10 (API 29) as well as Android 11+. Compatibility hooks cover framework calls that Android 10 lacks. These include the Compose drawing crash (`View.isShowingLayoutBounds`) that a DiLink 3 user reported. Rarely used screens may still reach newer-Android code on Android 10.
- **DiLink 3 instrument-cluster map (experimental, Android 10 only).** Enable it in **Instrument cluster → DiLink 3 cluster test → Enable DiLink 3 map projection**. It uses local ADB to switch the cluster into projection mode: keep ADB enabled and approve the Maps key. If Maps restarts while projection was active, projection pauses until you press **Authorize or retry**. This deliberate safety step follows earlier crash loops. **Restore stock cluster** returns the stock layout. HUD guidance is not supported on DiLink 3.
- **DiLink 5.1 instrument theme (experimental, not yet tested in a car).** **Instrument cluster → BYD instrument theme** appears only on two exact DiLink 5.1 / UI7 firmware builds. It offers a manual layer (Map, Scenario or Simple) or automatic following of the stock theme through Usage Access, a one-time ADB setup. Native zoom controls require automatic following.

Everything else from preview 2 is unchanged: interface size, 3D buildings, Search alignment, rounded corners, and the optional battery reader and EV route planning. An experimental cluster pointer-size option was not included: it had no visible effect on the tested car.

## Install or update

Use only the clean original **Maps Automotive 26.09.280001.E**, version code **1068809778**, ARM64:

```text
a9ecc4c18fac089c3c55fd20bed65233b7ea4db029da9b824305bf0b4b5716c9
```

The phone Maps APK, an already patched APK and other Maps versions are not supported input. Obtain the original yourself; it is not supplied here. Do not bypass compatibility checks with force or continue-on-error.

- [Add our TEST source to Morphe](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29), or add `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches` in **Sources → + → Remote**.
- [Local patch bundle](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.3/maps-automotive-for-byd-morphe-preview.3.mpp) · SHA-256: `b2b402d28c58f3c8af3867fe13031e4b7e3d8700a21fc37c8580c1149527b9eb`.
- Download [Morphe Manager](https://github.com/MorpheApp/morphe-manager/releases), [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop/releases) and [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) from their official Releases. MicroG-RE 7.2.1 is the tested version.

If you already use a personal signing key, **reuse it** to update without removing Automotive data. Changing keys requires a one-time reinstall and loses local Automotive data/settings. Keep the keystore and password backed up.

Morphe can refresh our patch source. It does **not** update installed Maps: patch the supported original again and install the APK signed with your same key. There is no in-app APK OTA.

## Verification and its limits

| Revision | Evidence |
| --- | --- |
| **52, this release** | Installed on one BYD Tang (DiLink 5.0) as an update, keeping app data and accounts. The owner checked during active navigation: pointer offset and cluster zoom work, the screen does not rotate, and the second custom-button press restores volume. Applied with Morphe Desktop 1.18.1, the public bundle produces an APK payload identical to that car-tested build. Host tests of every merged feature line passed, and written-DEX checks confirm that only reviewed Google methods are changed. This was not a full regression of every feature on the car. |
| **38 — steering-wheel zoom** | On the Tang the owner confirmed custom button → +/− zoom, zoom kept after a pause, and volume restored by the second press. |
| **36 / 50 — Android 10** | On an Android 10 (API 29) emulator: installation as an update with data kept, start-up, map, Google sign-in and search. The Compose crash from the DiLink 3 report was fixed from its stack trace and a static audit. The fix is **not yet confirmed on a DiLink 3 car**. |
| **40 / 41 — DiLink 3 cluster** | A tester's photos showed the map and route on the cluster of a DiLink 3 hybrid (revision 40). The later recovery behaviour has not yet been confirmed by a tester. |
| **39 — DiLink 5.1** | Host tests only; **no vehicle test yet**. |
| **35 / 42 — vehicle profile** | Host tests only; PHEV/HEV handling has no vehicle test. |
| **Preview 2 features** | Unchanged code; their earlier evidence is in the [preview 2 notes](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.2). |

The public builder pins **Morphe Desktop 1.18.1**. Preview 3 has not been retested in Android Morphe Manager; Manager 1.35.0 / API32 evidence belongs to preview 1. HUD on DiLink 3, other BYD firmware, DiLink 5.1 vehicles and other Android/tool combinations need further checks. The cluster arrow may stay in its navigation style after a route ends; this is under investigation.

## Optional vehicle integration

Basic Maps use does not require ADB. For live battery/range, enable network ADB, turn on both reader opt-ins under **Instrument cluster → BYD vehicle data**, and authorize the local reader with **Always allow**. Allow Maps startup/background operation in BYD settings.

EV planning additionally requires the `known_bev` patch option for a confirmed BEV, plus your own battery capacity in the vehicle profile. It is not PHEV support.

[Installation and feature guide](README.md) · [Migration guide](MIGRATION.md) · [Preview 3 release](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.3)
