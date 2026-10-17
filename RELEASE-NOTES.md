# Maps Automotive for BYD — Morphe preview 1

**Release tag: `morphe-preview.1` · patch revision26.**

This is our first test preview distributed as Morphe patches. It uses the Automotive edition of Google Maps, built for Android automotive systems, with our Instrument cluster controls and optional BYD battery/range integration. You create and sign the Maps APK locally from the matching original; no patched Maps APK is distributed in this preview.

Google sign-in uses MicroG-RE 7.2.1 independently of the phone edition of Google Maps for BYD. The Automotive package remains separate, so both Maps editions can be installed together.

Select **Maps Automotive for BYD (MicroG-RE TEST)** in Morphe. This public bundle is revision26 (`patch-lab.26` inside the app); its runtime matches the tested Morphe25 apart from the version marker. The car results below are evidence from25, not a repeated reboot/route test of26.

## Included

- A [Remote patch source](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29) for Morphe: add `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches` in **Sources → + → Remote**. It serves **TEST previews only**, including the default main feed. The existing [Local `.mpp` download](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.1/maps-automotive-for-byd-morphe-preview.1.mpp) remains available as a fallback.
- MicroG-RE provider checks with an explanation when the required provider is missing or unsuitable.
- Instrument cluster controls, route-card placement and map-style settings UI.
- Optional read-only BYD battery/range reader, firmware selection and conservative Unknown/Unverified diagnostics.
- One-minute sampling with 90-second expiry; stale values are withdrawn from EV predictions.
- Optional automatic reader startup using the head unit's local ADB service, after initial authorization and BYD background/startup permission.
- Experimental native EV planning for confirmed BEVs using CCS2 stations, with owner-entered battery capacity.

## Patch updates versus Maps updates

Morphe can refresh the Remote `.mpp` source automatically according to its update settings or through the source-card **Update** action. Updating the installed Maps app still means repatching the supported clean original and installing the output with your same signing key. There is no in-app APK OTA or automatic migration from old Automotive.

Only our source's patch should be selected for this app. The feed and `.mpp` downloads have been checked. Remote import, the source card and patch/version details passed in Manager 1.35.0 on clean Android API32 with Pre-release OFF. That check did not repeat patching; the completed Local patch/export/install test below is separate evidence. Automatic downloading of the correct original AAOS APK has not been verified: use **No, I already have an APK** and supply the exact original file.

## Tested on one BYD Tang

Morphe25 retained the account/profile and received live charge/range. With BYD background permission restored, the reader started on its own 28.67 seconds after a full head-unit reboot while the computer was disconnected. Fresh 91% / 548 km readings were captured before the computer reconnected.

A new route preview after that reboot planned **907 km to Paris with two automatic charging stops and 30% at arrival**. No charging stops were supplied manually and navigation was not started. The first displayed stop showed 17% arrival / 83% departure. The second stop's details were not inspected. The test profile was Tang, 108 kWh entered by the owner, CCS2 through an adapter.

## Before installing

Use only original Maps Automotive **26.09.280001.E**, code **1068809778**, ARM64. Its tested SHA-256 is `a9ecc4c18fac089c3c55fd20bed65233b7ea4db029da9b824305bf0b4b5716c9`. Morphe Desktop 1.18.1 CLI and actual Manager 1.35.0 patch/export on Android API32 are confirmed. The exported APK installed separately through ADB and passed first-run, missing-MicroG-RE guard/Not now, and settings checks. Its signature/alignment, manifest, original class preservation and written EV/navigation/cluster hooks passed. This fresh emulator had no Google account; it did not prove sign-in, map tiles or vehicle integration. Use Manager **Save**, then install the exported APK through Android's file manager. The Manager **Install** button reported Installed without installing the package in this test.

Back up your own signing key and its password. Your locally signed output normally cannot update the existing public `automotive.6` APK because its signer differs. Reinstalling for this migration removes local Automotive data/settings. The old updater does not perform this migration. MicroG-RE requires its own Google sign-in.

For automatic reading, enable both Maps reader opt-ins, authorize its ADB key with **Always allow**, and allow Maps background/startup execution in BYD settings. On the tested Tang, **Disable background Apps → Maps Automotive must be OFF**. This is separate from the two Maps switches, which stay ON.

## Known limits

Only one Tang firmware has device evidence. Route-preview results are not driving validation; charging power/duration and charging-inclusive ETA are not verified. BEV/CCS2 integration is opt-in, not PHEV or universal connector support. Physical cluster/HUD/audio, current-bundle PiP and other Manager/Android/device combinations require further validation. Morphe cluster satellite imagery remains unresolved. The general native navigation-service startup timeout is not declared solved. Cover and lock-feedback codes remain unverified physical indicators. Morphe can refresh the Remote patch bundle; installed Maps updates still require repatching and installation with the same key. In-app APK OTA is not provided.

Patch bundle: [maps-automotive-for-byd-morphe-preview.1.mpp](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.1/maps-automotive-for-byd-morphe-preview.1.mpp)
Patch SHA-256: `4be9eecff4c37199d06e3474a1d416dec8107f685c189d51daa4e4b669bca3bf`
Installation guide: [Read the guide](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/blob/main/README.md)
Release notes: [Morphe preview 1](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.1)
