# Maps Automotive for BYD — Morphe test preview

**Morphe preview 3 · patch revision 52 · release tag `morphe-preview.3`.**

We have ended development and support for **our phone edition of Google Maps for BYD**. Future work focuses on Automotive. Start with the [migration and self-build guide](MIGRATION.md): it explains which APK to use, how to build it in Morphe, and when an old installation needs replacing. **Phone Maps can remain installed** alongside the new Automotive app.

This is the Automotive edition of Google Maps, designed for Android automotive systems. It is a different app from the phone edition. Our patches add the Instrument cluster controls and optional BYD battery/range integration.

This **GitHub repository and its Releases distribute only our own patches (`.mpp`) and code**, without a Maps APK. For self-build, apply the patches to your own matching original APK and sign the result locally, using Morphe Manager on Android or the Desktop workflow. A project-signed **[ready APK — preview 2 / build 34](READY-APK-preview.2.md)** is available as a [Telegram attachment](https://t.me/byd_apps_updates/29), for confirmed BEVs using CCS2 (TEST); preview 3 is self-build only for now. Existing self-build users should reuse their key; moving between a personal key and the project-signed APK requires the one-time reinstall described below. Google sign-in uses **MicroG-RE (7.2.1 tested)**; the phone edition of Google Maps for BYD is not required. The Automotive package is `com.google.android.apps.maps.automotive`, so it can coexist with the phone edition.

The patch is named **Maps Automotive for BYD (MicroG-RE TEST)** in Morphe. The app's diagnostic version is `patch-lab.52-unified`; the public release name is Morphe preview 3. This update adds instrument-cluster controls and experimental Android 10 / DiLink 3 / DiLink 5.1 support; the preview 2 interface features and the optional reader are unchanged. Earlier car results below are identified by their actual tested revision.

![Illustrative native 3D settings on an account-free Android API32 test emulator](https://raw.githubusercontent.com/romanchukg-cloud/maps-automotive-for-byd-patches/main/docs/morphe-preview.2.png)

*Revision 29 screenshot from an account-free API32 laboratory emulator, illustrating the native 3D settings UI that is still included. It is not a revision 52 test or proof of rendered buildings, live vehicle readings or physical cluster output.*

## New in preview 3

- **Steering-wheel zoom for the cluster map:** enable **Instrument cluster → Steering-wheel zoom**. Press the custom steering-wheel button, then volume + or − zooms the instrument-cluster map; press the custom button again to restore volume. The zoom stays until **Reset cluster zoom** or until the cluster map is recreated. Setup uses ADB on the head unit and a key-only accessibility service that does not read screen content. Default buttons are for the BYD Tang. OFF by default.
- **Pointer and zoom during navigation:** **Map layout → Pointer vertical offset** now also moves the navigation arrow during active guidance, and cluster zoom works in navigation without rotating the screen.
- **Vehicle profile:** **BYD vehicle data → Vehicle profile** offers Unknown / BEV / PHEV / HEV, a name, usable capacity and your actual connectors. EV route planning remains BEV-only.
- **Android 10 (experimental):** the patch now accepts Android 10 (API 29), with compatibility hooks that include the fix for a Compose drawing crash reported on DiLink 3.
- **DiLink 3 cluster map (experimental, Android 10 only)** and **DiLink 5.1 instrument theme (experimental, two exact firmware builds, not yet tested in a car)** — see the optional sections below.

Revision 52 was installed on one BYD Tang as an update with data kept. During active navigation the owner confirmed: pointer offset and cluster zoom work, the screen does not rotate, and volume returns after the second custom-button press. The public bundle produces an APK payload identical to that car-tested build. See [release notes](RELEASE-NOTES.md) for the per-feature evidence and limits.

## What you need

- An ARM64 Android head unit running Android 10 or newer. Android 10 support is experimental. Device testing so far is mainly on one BYD Tang; other firmware is not confirmed.
- The original **Maps Automotive 26.09.280001.E**, version code **1068809778**, ARM64 APK. The phone Maps APK and other versions are not interchangeable with this input. Obtain your own original; this project does not distribute it.
- Either [Morphe Manager for Android](https://github.com/MorpheApp/morphe-manager/releases), or [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop/releases) with Java and Android SDK Build Tools. Download the application from its official Releases page.
- [MicroG-RE — official Releases](https://github.com/MorpheApp/MicroG-RE/releases). Choose the matching **ARM64 APK**. This preview requires at least 7.2.1, which is the tested version; newer releases are not automatically verified. Its package is `app.revanced.android.gms`; ordinary microG under `com.google.android.gms` does not replace this dependency.
- Our patch source: [Add Maps Automotive for BYD (TEST) to Morphe](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29), or add `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches` in **Sources → + → Remote**. The source currently serves **TEST previews only**, including its default main channel; this is not a stable channel.
- Optional Local/CLI fallback bundle: [maps-automotive-for-byd-morphe-preview.3.mpp](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/download/morphe-preview.3/maps-automotive-for-byd-morphe-preview.3.mpp). Its SHA-256 is `b2b402d28c58f3c8af3867fe13031e4b7e3d8700a21fc37c8580c1149527b9eb`.

The Desktop workflow uses **Morphe Desktop 1.18.1**. **Morphe Manager 1.35.0 on Android API32 was verified for preview 1**: patch/export, separate APK installation, provider guard, settings and Remote import passed. Preview 2 and preview 3 bundles have not been retested in Android Manager. These are recorded tool versions, not a requirement to use fixed download links or a claim that later releases are verified; newer releases can change their UI or CLI.

The exact tested original APK SHA-256 is:

```text
a9ecc4c18fac089c3c55fd20bed65233b7ea4db029da9b824305bf0b4b5716c9
```

The tested MicroG-RE **7.2.1 ARM64** download SHA-256 is (not the hash of future releases):

```text
3425e46e95cd45012892cfaa23061b70db0a25affffcfcb84e3df4511d63eb82
```

## Can I patch the Maps already installed on my head unit?

Morphe needs the **clean original Maps Automotive 26.09.280001.E ARM64 APK file**, version code 1068809778, matching the SHA-256 above. Installed phone Maps, APKs exported from phone Maps, old Automotive for BYD and already patched APKs are not supported input. You do not need to install the original file before patching it. Both the correct original and phone Maps can use `com.google.android.apps.maps`, so the package name alone is not enough to identify the right input.

The patched output uses `com.google.android.apps.maps.automotive` and can coexist with phone Maps. A different signer affects an existing **Automotive** installation: a one-time reinstall loses its local data/settings. Do not remove phone Maps or older microG to resolve that mismatch. Keep older microG if other apps use it; MicroG-RE needs a separate sign-in. See the [complete migration steps](MIGRATION.md).

## First installation and migration

1. Keep your existing Maps working while you prepare the new output APK. Save the settings you want to reproduce.
2. Install the matching ARM64 APK from [MicroG-RE Releases](https://github.com/MorpheApp/MicroG-RE/releases) and add your Google account to it. Version 7.2.1 is tested; newer releases need their own verification. This is a separate sign-in from older microG. Do not remove another Google-services provider or its accounts as part of these steps.
3. Apply the release `.mpp` to the exact original APK using the Manager steps below, or the Desktop fallback. Select **Maps Automotive for BYD (MicroG-RE TEST)**. Leave the patch options at their defaults for ordinary map use. Enable `known_bev` only for a confirmed battery electric vehicle when you intend to use the experimental CCS2 EV routing integration below. It is not a PHEV mode.
4. Manager signs with a key it creates for you; export a private backup as described below. With Desktop, sign using **your own key**. Keep the keystore and its password backed up separately, and reuse that same key for every later patch update.
5. Install the APK you created. A public `automotive.6` installation uses a different signing key: Android will normally reject installing your locally signed APK over it. Migrating then requires uninstalling the old Automotive app and installing yours, which removes its local data/settings. Your regular phone Maps is a separate app. Do not uninstall it or MicroG-RE to solve this signature mismatch.
6. Open Maps Automotive, grant Contacts and Location, then finish Google sign-in if asked. Check the map and account before setting up vehicle data.

Do not use a key from someone else's APK or expect the old in-app APK updater to convert the installation. Automatic migration from `automotive.6` to this patch preview is not provided. For later updates, patch the supported original again and sign with the same backed-up key; same package and signer are required to retain local data.

### Android: Morphe Manager

1. Install Morphe Manager and copy the matching clean original Maps Automotive APK to that Android device. Patching on a phone and transferring the exported APK to the car is possible; leave architecture optimization off.
2. **Preferred: Remote source.** Open [Add Maps Automotive for BYD (TEST)](https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29) and confirm, or open **Home → Sources → + → Remote** and paste `https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches`. Manager fetches the patch bundle for you. **Local fallback:** download the `.mpp`, then use **Sources → + → Local** and select it. A local source does not update itself; replace its bundle manually.
3. Open **Settings → Advanced → Expert settings → Expert mode** and enable it. Keep **Optimize for device architecture OFF**.
4. Choose **Maps Automotive 26.09 TEST** from our source. If Morphe offers several sources, select ours and leave patches from other sources unselected for this app. Select **26.09.280001.E → No, I already have an APK**, then select your clean original APK. Automatic original-APK Download has not been verified for this AAOS version. Do not choose a phone Maps APK or an already patched output.
5. Enable only **Maps Automotive for BYD (MicroG-RE TEST)**. Open its settings icon for **Battery electric vehicle (CCS2)** (`known_bev`). This option defaults OFF; turn it ON only for a confirmed BEV using CCS2 station planning. It does not enter your battery capacity or enable vehicle reading.
6. Choose **Proceed to patching** and keep Manager in the foreground. After success, choose **Save** to export the signed APK. Transfer that APK to the car, then open it with the normal Android file manager to install. Do not rely on Manager's **Install** status: in this API32 test it reported Installed while the package was absent; installing the exported file separately succeeded through ADB.
7. Back up **Settings → System → Import & export → Signing key → Export** separately from your ordinary Manager settings. Import that same signing key when moving to another patching device. A replacement key cannot update an app signed with the old one.

For **preview 1**, on the disposable Android API32 emulator, actual patching, Save/export and a separate ADB installation succeeded. With MicroG-RE absent, the app displayed the required-provider explanation and **Not now** dismissed it without a crash. After installing fresh MicroG-RE 7.2.1 without an account, first-run and Instrument cluster settings opened. The exported APK also passed signature/alignment, manifest, original-class preservation and written EV/navigation/cluster hook checks. This clean-emulator test did not sign in to Google or prove map tiles, live vehicle data or physical cluster output. A Manager-generated signer differs from the existing public Automotive signer; the same migration and data-loss warning above applies. Patching on the head unit itself has not been established across BYD firmware.

### Updating the source and Maps

Our remote source is **TEST only**, including the default main feed. Morphe can refresh its `.mpp` automatically according to **Settings → Advanced → Updates**, or you can choose **Update** on the source card. This downloads patch updates inside Morphe; it does not update the Maps installation by itself.

To update Maps, run patching again with the supported clean original, keep your **same signing key**, choose **Save**, and install the resulting APK. There is no in-app APK OTA or automatic migration from the old Automotive builds. Local `.mpp` imports still need a manual replacement file. Remote import, the source card and patch/version details were checked for **preview 1** in Manager 1.35.0 on clean Android API32 with Pre-release OFF. That Remote check did not repeat patching; the completed preview 1 Local patch/export/install checks above are separate evidence. Preview 3 still needs its own Manager import/patch/export verification.

### Computer fallback: Morphe Desktop CLI

Use the downloaded filenames in place of the example filenames below; `morphe-desktop.jar` means the Desktop JAR you downloaded from its official Releases page. The command was verified with Desktop 1.18.1, and a later release may change CLI options. Keep the original input unchanged. The first command checks the original APK's signature; also compare its SHA-256 with the value above.

```sh
apksigner verify original-maps-automotive.apk
java -Xmx2g -jar morphe-desktop.jar patch --unsigned -e "Maps Automotive for BYD (MicroG-RE TEST)" -p maps-automotive-for-byd-morphe-preview.3.mpp -o maps-automotive-unsigned.apk -r patch-result.json -t patch-scratch original-maps-automotive.apk
```

For a confirmed BEV using CCS2 station planning, add `-O known_bev=true` to the patch command. Do not add `--force` or `--continue-on-error`; a failed compatibility check means the input or tool combination needs checking.

The output above is unsigned. If you do not already have your own key for this package, create one **once**, using interactive password entry:

```sh
keytool -genkeypair -keystore maps-automotive-user.keystore -alias maps-automotive -keyalg RSA -keysize 4096 -validity 10000
```

Back up that keystore and its password separately before continuing. If you already have a signing key for this package, skip key creation and use its filename/alias in the following commands. Android SDK Build Tools36.0.0 were used for this signing/alignment workflow:

```sh
zipalign -P 16 4 maps-automotive-unsigned.apk maps-automotive-aligned.apk
apksigner sign --ks maps-automotive-user.keystore --ks-key-alias maps-automotive --out maps-automotive.apk maps-automotive-aligned.apk
apksigner verify --print-certs maps-automotive.apk
zipalign -c -P 16 4 maps-automotive.apk
```

Use interactive password entry; do not put a keystore password in a shared command, screenshot or support report. Keep the same key for later updates. Morphe's signing workflow may also be used if it keeps the same exportable key between updates. No production or laboratory signing key is supplied by this project.

## Optional: BYD battery and range

Basic Maps use does not require this setup. Both vehicle-data opt-ins start **off** on a new installation.

1. Enable network ADB in the head unit's settings. The reader connects locally to `127.0.0.1:5555`; ADB must remain enabled after a head-unit reboot.
2. In BYD settings, allow Maps Automotive automatic startup and background operation. On the tested Tang the menu is **Disable background Apps**: the **Maps Automotive switch must be OFF** there, because it is a list of blocked apps. Other firmware may use different labels or switch directions. Recheck this permission after updates.
3. Open **Maps Automotive → Instrument cluster → BYD vehicle data** and enable **Use BYD vehicle data**.
4. Open **Automatic reader → Enable automatic reader**. Approve the standard ADB authorization prompt on the head unit and select **Always allow**. If needed, use **Authorize or retry**.
5. Check for **Automatic reader connected** and fresh battery charge/range readings that match the instrument cluster.

After initial setup, the reader can start without a connected computer. Morphe revision 25 did this after a complete head-unit reboot on the tested Tang. The reader code is unchanged since preview 1, but revision 52 has not had a new reboot test. Readings update about once a minute and expire after 90 seconds. The reader only reads vehicle data; it does not operate the charging port or change vehicle settings.

Leave **Reader compatibility → Automatic CAN/CANFD** selected initially. Unsupported firmware can show Unknown or Unsupported. The advanced firmware-detection mode requires comparing its fresh candidate charge and range with the physical instrument cluster, then explicitly confirming them before EV routing can use them. Do not confirm guesses.

## Optional: experimental EV route planning

This preview's route integration is for **battery electric vehicles using CCS2 charging stations**. It has not been validated for PHEVs or other station connectors. The `known_bev` patch option must have been enabled when creating the APK; it does not turn on the reader or fill in your battery capacity.

Once charge and range are fresh and correct, open **BYD vehicle data → Vehicle profile**. Choose **BEV**, enter your own usable battery capacity in **kWh** and the connectors you use, then **Save vehicle profile** and enable EV routes. After changing the type or connector, Force stop and reopen Maps. The owner test used a BYD Tang with a user-entered 108 kWh capacity and CCS2 stations through an adapter. Use your vehicle's capacity, not this example value.

With the profile enabled, fresh accepted charge/range data are supplied to Maps for its native EV route calculation. In a **Morphe revision 25** stationary test after a full reboot, Maps planned a 907 km route to Paris with **two automatic charging stops and 30% at arrival**, using the car's 91% / 548 km readings. We did not add charger waypoints manually or start driving the route.

These are route-preview results, not a road validation of battery predictions. Charging power and charging duration are not supplied or verified. Do not interpret a displayed journey time as a tested estimate including charging time. When readings expire, the integration withdraws the battery prediction rather than continuing with old data.

## Optional: DiLink 3 instrument-cluster map (experimental, Android 10 only)

1. Keep network ADB enabled; the projection uses the head unit's local ADB.
2. Open **Instrument cluster → DiLink 3 cluster test → Enable DiLink 3 map projection**, confirm **Start test** while parked, and approve the Maps ADB key if prompted. Keep **Show cluster map** enabled.
3. **Projection status** shows the current state. **Restore stock cluster** pauses projection and returns the stock instrument layout; turn the option off after testing to keep it off on the next launch.

If Maps restarts while projection was active, projection deliberately stays paused until you press **Authorize or retry**. This prevents a crash loop after an unclean session. HUD guidance is not supported on DiLink 3. One tester's photos showed the map and route on a DiLink 3 hybrid cluster (revision 40); later revisions are not yet confirmed in a car.

## Optional: DiLink 5.1 instrument theme (experimental, not yet tested in a car)

**Instrument cluster → BYD instrument theme** appears only on two exact DiLink 5.1 / UI7 firmware builds. Choose a manual layer (**Map**, **Scenario** or **Simple**), or enable **Follow instrument theme** with the one-time **Allow Usage Access · ADB** setup so Maps follows the stock cluster theme. Unknown visibility hides the map and releases wheel controls; native zoom needs automatic following. This profile has host tests only.

## Sign-in troubleshooting on the tested Tang

If MicroG-RE fails before showing the email form, a tested workaround on this Tang was to select its built-in **Google Pixel 5 (Android 13)** device profile with **Authenticate with device registration** disabled. Those settings affect how MicroG-RE presents the device to Google. They are a device-specific workaround, not a confirmed requirement for every head unit. Changing the profile fixed registration in this case; the separate necessity of disabling authentication was not isolated.

## Preview limits

- Revision 52 was checked on one BYD Tang during active navigation: pointer offset, cluster zoom, no screen rotation and volume restore. It was not a full account, route, reboot and UI regression of every feature.
- Earlier focused tests remain separate evidence: steering-wheel zoom on revision 38; 3D buildings on revisions 30/31; scale, automatic application and search alignment on revisions 32/33. These do not establish every behavior of 52 on every device.
- Android 10 / DiLink 3 and DiLink 5.1 support is experimental. The DiLink 3 Compose crash fix was made from a reported stack trace and is not yet confirmed in that car. DiLink 5.1 has no vehicle test. Rarely used screens may still reach newer-Android code on Android 10.
- Manager 1.35.0/API32 import, patch/export and first-run evidence belongs to preview 1; preview 3 has not been retested in Android Manager.
- HUD on DiLink 3, satellite imagery in the Morphe cluster and current-bundle PiP need further work. The cluster arrow may keep its navigation style after a route ends; this is under investigation. A general native navigation-service startup timeout remains unresolved.
- Reader/reboot and Paris charging-stop evidence belongs to Morphe revision 25. No new road validation, charging-power/duration test or charging-inclusive ETA guarantee is claimed. PHEV/HEV profiles have no fuel or combined energy model.
- Morphe updates the patch source. Maps still requires repatching and installation with the same backed-up key; there is no in-app APK OTA. Automatic downloading of the correct original AAOS APK has not been verified.

[Release notes](https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/releases/tag/morphe-preview.3).
