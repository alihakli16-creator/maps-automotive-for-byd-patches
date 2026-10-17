# Moving from Google Maps for BYD phone edition to Automotive

We have ended development and support for **our phone edition of Google Maps for BYD**. Our work now focuses on the **Automotive edition**, designed for Android automotive systems. You can keep your existing phone edition installed while you try Automotive; it is not required by the new version.

The current test release is **Morphe preview 1**. It provides our `.mpp` patches. You create the Maps APK locally by applying them to the matching clean original Maps Automotive APK. Google sign-in is provided by **MicroG-RE 7.2.1**, independently of phone Maps.

## Download these files

- [Morphe Manager for Android — official Releases](https://github.com/MorpheApp/morphe-manager/releases).
- [Our Morphe preview 1 patch bundle](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.1/maps-automotive-for-byd-morphe-preview.1.mpp).
- [MicroG-RE 7.2.1 for ARM64 — official download](https://github.com/MorpheApp/MicroG-RE/releases/download/7.2.1/microg-7.2.1-icon-arm64-v8a.apk).
- Your own **clean original Maps Automotive 26.09.280001.E**, version code **1068809778**, ARM64 APK file. We do not distribute Google's original APK. Obtain the matching original file yourself and compare it with the exact input below.

The resulting Automotive app requires an ARM64 head unit with Android 11 or newer. You can patch on an Android device and transfer the result to the head unit. The workflow was tested with **Morphe Manager 1.35.0 on Android API32**; a newer release is not automatically verified and can change its UI. On-head-unit patching performance is not established across BYD firmware. For a computer, use [Morphe Desktop from its official Releases page](https://github.com/MorpheApp/morphe-desktop/releases) and the [Desktop fallback guide](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/blob/main/README.md#computer-fallback-morphe-desktop-cli), tested with Desktop 1.18.1.

## Which Maps file should I select?

| File | Use as Morphe input? |
| --- | --- |
| Clean original **Maps Automotive 26.09.280001.E**, code 1068809778, ARM64, matching the hash below | **Yes** |
| Your installed phone Google Maps / Google Maps for BYD, or an APK exported from it | **No** |
| Old Automotive for BYD, an APK extracted from that installation, or an already patched Morphe output | **No** |
| Another Maps Automotive version or architecture | **No** |

The clean input's package is `com.google.android.apps.maps`. The phone edition can use that same package name, so the package name alone does **not** identify the correct file. Check the Automotive version, version code, architecture, original Google signature and exact SHA-256:

```text
a9ecc4c18fac089c3c55fd20bed65233b7ea4db029da9b824305bf0b4b5716c9
```

Patches are applied to this **APK file**. You do not patch the phone app already installed on the head unit, and you do not need to install the clean original before patching it. Do not use force/continue-on-error if the input is rejected.

## Build your Automotive APK with Morphe Manager

1. Install **Morphe Manager**. Copy the clean original APK and our `.mpp` file to that Android device.
2. Open **Home → Sources → + → Local** and select `maps-automotive-for-byd-morphe-preview.1.mpp`. Choose the Automotive app if an app selection is offered.
3. Open **Settings → Advanced → Expert settings → Expert mode** and enable it. Leave **Optimize for device architecture OFF**, especially when preparing the APK on a different device from the car.
4. Select Google Maps from our local source, then **26.09.280001.E → No, I already have an APK**. Choose the clean original file described above.
5. Select only **Maps Automotive for BYD (MicroG-RE TEST)**. For ordinary map use, leave its **Battery electric vehicle (CCS2)** option OFF. Enable that option only for a confirmed battery electric vehicle when you want the experimental CCS2 route integration; it is not a PHEV mode.
6. Choose **Proceed to patching** and keep Manager in the foreground. When it finishes, choose **Save** to export the signed APK. Transfer the saved APK to the head unit. Install it by opening the file with the normal Android file manager; do not rely on Manager's Install status. That button reported Installed without installing in the API32 test; a separate installation of the exported file succeeded through ADB.
7. **Back up the signing key:** open **Settings → System → Import & export → Signing key → Export** in Manager. Keep that private backup separately from Manager settings. Import the same key before patching on another device. Do not create a new signing identity for every update.

Morphe Manager creates a signing key for your output. This personal key is what makes your future updates compatible with your installation. The project does not supply its private production key.

## Install alongside phone Maps, or replace old Automotive

**Phone Maps may stay installed.** The patched Automotive output uses `com.google.android.apps.maps.automotive`; the phone edition uses `com.google.android.apps.maps`. They are separate installations. Removing phone Maps is optional after you have checked the new Automotive app; it is not a prerequisite.

If you already have an older **Automotive** build such as `automotive.6`, your new locally signed APK normally has a different signing key. Android cannot update that old installation with the new key. Note the Automotive settings you want to reproduce, uninstall **only the old Automotive app**, then install your locally created APK. This migration removes that Automotive app's local data and settings. Do not uninstall phone Maps or microG to solve an Automotive signature mismatch.

For later patch updates, use the supported clean original again, apply the new `.mpp`, and sign with the **same backed-up key**. Matching package and signer allow an update instead of another reinstall. This preview does not provide an automatic OTA migration from old Automotive or an automatic update channel for local `.mpp` sources.

## Google sign-in

1. Install the supplied official **MicroG-RE 7.2.1** ARM64 download. It uses package `app.revanced.android.gms` and is separate from older microG under `com.google.android.gms`.
2. Add your Google account to **MicroG-RE**. An account in older microG does not automatically become a MicroG-RE account.
3. Open your new Maps Automotive APK. Allow Contacts and Location and finish sign-in if requested.
4. Check that the map and your account work before changing anything else.

**Keep older microG if other apps need it.** Do not remove its accounts as part of this migration. Phone Maps is not used as an account helper by the new Automotive app.

If MicroG-RE fails before the email form on a Tang, see the device-specific workaround in the [full project guide](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/blob/main/README.md#sign-in-troubleshooting-on-the-tested-tang). It is not a universal setup requirement.

## Optional BYD battery/range and EV routes

Ordinary Maps use does not require the ADB reader. Its two opt-ins start OFF on a new installation.

If you want live battery/range data, enable network ADB on the head unit, then turn on **Instrument cluster → BYD vehicle data → Use BYD vehicle data** and **Automatic reader → Enable automatic reader**. Approve the standard ADB prompt and select **Always allow**. Also allow Maps startup/background operation in BYD settings. On the tested Tang, **Disable background Apps → Maps Automotive must be OFF**, because that menu lists blocked apps; both Maps reader opt-ins stay ON.

After that setup, the reader can start without a computer. It samples about once a minute; values expire after 90 seconds. Experimental native EV planning additionally needs the confirmed-BEV/CCS2 patch option and your own battery capacity in **BYD vehicle data → EV routing profile**. Other firmware, PHEVs and other station connectors are not confirmed. Read the [full setup and limitations](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/blob/main/README.md) before enabling this optional feature.

## What has been checked

Morphe25 on one BYD Tang supplied fresh charge/range after a full head-unit reboot without a computer and produced a Paris route preview with two automatic charging stops. The public revision26 retains that runtime apart from its version marker; its own clean Manager API32 test checked patch/export, separate APK installation, missing-provider handling and settings. The clean emulator had no Google account and did not prove map tiles, live car data or physical cluster output. Road accuracy, other BYD firmware and the remaining limits are listed in the release notes.

[Full guide and Desktop fallback](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/blob/main/README.md) · [Morphe preview 1 release notes](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.1)
