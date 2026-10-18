# Maps Automotive for BYD — Morphe preview 2

**Release tag: `morphe-preview.2` · patch revision 34 · diagnostic version `patch-lab.34`.**

This test update makes the Automotive Maps interface more adjustable: smaller controls, automatic size changes, a 3D buildings switch and optional rounded app corners. It also aligns the collapsed search bar with the toolbar.

**GitHub supplies our own patches and code only.** Apply the `.mpp` to the exact original Maps Automotive APK below, then sign locally. The previously supplied [Telegram ready APK](READY-APK.md) is still **preview 1 / build 26**, without these additions. For preview 2, use the updated patches and keep your existing signing key.

## Changes since preview 1

- **Interface size — 100%, 90% or 80%.** Choose **Instrument cluster → Interface size**. A new installation defaults to 90%; existing explicit choices are preserved. This changes the main Maps interface, leaving the head unit's system density and the cluster/HUD scale unchanged.
- **Size changes apply automatically.** When navigation is known to be inactive, selecting a size refreshes Maps without a manual Apply button or restart. During active navigation, or while its state is unknown, the selection is saved and applied when navigation is known inactive.
- **Native 3D buildings switch.** Maps settings expose **Raised Buildings / 3D Buildings** to turn building extrusion on or off. This retains the native rendering, layer and thermal rules; it is not a new 3D navigation mode.
- **Aligned collapsed Search.** The search bar now aligns with the top toolbar buttons at the supported sizes; the expanded search panel keeps its native spacing.
- **Optional rounded app corners.** **Instrument cluster → Rounded app corners** adds black 28dp outer corners to eligible central-screen Maps windows. It starts **OFF**. The mask does not change touch targets or layout; the cluster/HUD and BYD system bars are outside its scope.

The optional battery/range reader, experimental EV routing and automatic reader startup are unchanged. They remain opt-in, with the same setup and limits.

## Install or update

Use only the clean original **Maps Automotive 26.09.280001.E**, version code **1068809778**, ARM64:

```text
a9ecc4c18fac089c3c55fd20bed65233b7ea4db029da9b824305bf0b4b5716c9
```

The phone Maps APK, an already patched APK and other Maps versions are not supported input. Obtain the original yourself; it is not supplied here. Do not bypass compatibility checks with force or continue-on-error.

- [Add our TEST source to Morphe](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29), or add `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches` under **Sources → + → Remote**. Select **Maps Automotive for BYD (MicroG-RE TEST)** only. The main feed also contains TEST previews.
- [Local patch bundle](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.2/maps-automotive-for-byd-morphe-preview.2.mpp) · SHA-256: `92dda796f402161a0cea9f57b02fee17972cb3f18bf0813b27e2eed96215b663`.
- Download [Morphe Manager](https://github.com/MorpheApp/morphe-manager/releases), [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop/releases) and [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases) from their official Releases pages. MicroG-RE 7.2.1 ARM64 is tested; newer versions are not automatically verified. It requires a separate Google sign-in, followed by Contacts and Location access in Maps.

If you already use a personal signing key, **reuse it** to update without removing Automotive data. Changing keys requires a one-time reinstall and loses local Automotive data/settings. Keep the keystore and password backed up separately. Phone Maps can stay installed; Automotive uses `com.google.android.apps.maps.automotive`. Do not remove phone Maps or another microG provider to solve a signature mismatch.

Morphe can refresh our patch source. It does **not** update installed Maps: patch the supported original again and install the APK signed with your same key. There is no in-app APK OTA. Automatic downloading of the correct original AAOS APK has not been verified; supply it manually.

## Verification and its limits

| Revision | Evidence |
| --- | --- |
| **34, included here** | Installed on one BYD Tang. The owner accepted the main-screen appearance and stopped further testing. Signed-APK packaging, source scope and written DEX checks passed. This was not a full account, route, reboot or UI regression test. |
| **32/33, included unchanged** | Tang checks covered collapsed Search alignment and automatic size changes, including saving a selection during navigation and applying it after navigation ended. These are earlier-revision checks, not a repeat on 34. |
| **30/31, 3D behavior included unchanged** | Emulator 30 and Tang 31 checks showed actual building extrusion change with the native switch. Earlier cold-launch checks on the emulator do not establish general startup reliability. |
| **25, inherited reader/EV behavior** | On one Tang, the reader supplied fresh data after a full head-unit reboot without a computer. A stationary 907 km Paris preview used 91% / 548 km readings and planned two charging stops with 30% at arrival. Navigation was not started. These tests were not repeated on 34. |

The public builder pins **Morphe Desktop 1.18.1**. Android **Manager 1.35.0 / API32** Remote import, Local patch/export, separate installation, provider guard and settings checks belong to **preview 1**. Preview 2 has not been retested in Android Manager. In the older test, Manager's Install button reported Installed while the package was absent; exporting with Save and installing separately succeeded. The clean Manager emulator had no Google account and did not prove tiles, live vehicle data or physical cluster output.

Rounded corners in night mode, RTL, other windows and together with scale changes remain unverified. Physical cluster/HUD/audio, current-bundle PiP, other BYD firmware and other Android/tool combinations need further checks. Morphe cluster satellite imagery and the general native navigation-service/foreground-service startup timeout remain unresolved.

## Optional vehicle integration

Basic Maps use does not require ADB. For live battery/range, enable network ADB, both reader opt-ins under **Instrument cluster → BYD vehicle data**, and authorize the local reader with **Always allow**. Allow Maps startup/background operation in BYD settings. On the tested Tang, **Disable background Apps → Maps Automotive must be OFF**, because this is a blocked-app list; the two Maps reader switches stay ON. Readings are sampled about once a minute and expire after 90 seconds.

EV planning additionally requires the `known_bev` patch option for a confirmed BEV using CCS2 stations and your own battery capacity in the EV profile. It is not verified PHEV or universal-connector support. The Tang test used an owner-entered 108 kWh and CCS2 through an adapter; use your vehicle's capacity. Road predictions, charging power/duration and charging-inclusive ETA remain unverified. Cover and port-lock feedback may show Unknown or Unverified and are not confirmed physical state indicators.

[Installation and feature guide](README.md) · [Migration guide](MIGRATION.md) · [Preview 2 release](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.2)
