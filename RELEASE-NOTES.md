# Morphe preview 6 — Gemini in Maps Automotive (TEST)

**Published 22 October 2026 · TEST preview.** One bundle contains our Maps 54N and optional Google 08 patches. GitHub distributes own patches/code/docs only; no Google APK. This is a test preview.

## Changes since preview 5

- Gemini button in Maps, below the cluster-map toggle; optional steering-wheel long microphone on the tested Tang.
- Choose **Conversation (Gemini Live)** or **Microphone**. One tap starts that voice mode in a compact panel when Google is the Android assistant.
- **Automatic compact recovery**, opt-in/default OFF. With Google self-start allowed in BYD, it recovers the compact assistant after Google force-stop through the head unit's approved local ADB. No computer is needed after setup. Another selected assistant is kept.
- Own optional Google app patch for MicroG-RE, native voice-mode selection, recognizer metadata compatibility and route handoff to Automotive.
- Maps EV-profile name is supplied to native VehicleInfo upload. The emulator accepted the changed name; **a changed name in the phone's connected-car list was not confirmed**.
- All preview 5 cluster/reader/profile features are retained.

## Installation

Follow [the short guide](GEMINI-SETUP.en.md) and [exact input/build instructions](GEMINI-BUILD.en.md). Maps uses **26.10.120001.E /1068833407**; Google uses the pinned **17.62.11.ve.arm64 /301813762 API 32+ APKM**. Sign **both apps with the same backed-up key**. Same-key Maps updates keep data. Replacing a differently signed Google app loses that app's local settings; do not clear Maps or microG accounts.

For Gemini use **MicroG-RE 7.2.2+ (7.2.2 tested)** and allow **Contacts/Location for MicroG-RE**. BYD **General → Apps → Disable Self-Start → Google = OFF**, Maps **Compact assistant ON**, **Automatic compact recovery ON**, local ADB enabled/Maps key **Always allow**. The current Google build needs **Android API 32+**; Maps-only DiLink 3/API 29 support does not make Gemini supported there.

Downloads: [Morphe Manager](https://github.com/MorpheApp/morphe-manager/releases), [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop/releases), [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases). Download the supported Google originals separately; they are not release assets here.

## Verification

- Rebuilt from the verified Maps 54N + Google 08 own source. Both extension DEX hashes exactly match those baselines.
- Applied separately with Desktop1.18.1 and compatibility checks. Every non-signature ZIP entry matches: **Maps 3264 entries**, **Google base 10536 entries**, including all DEX/resources/native libraries. Neither app has a new behavior change relative to its tested baseline.
- Host tests: **71** recovery-protocol checks and **301** Google provider/routing checks pass. Both named patches are discovered in one bundle.
- Portable Google helper patches/signs/verifies all 10 original splits with the existing project key; stock split payloads remain unchanged. It performs no install and creates no key.
- Owner on **Tang / DiLink 5.0 / API 32** confirmed screen and long-microphone one-tap Conversation with Maps 54L/Google 08. Maps 54N adds the recovery: owner Force stop Google→Home→Maps→voice button restored compact Conversation after allowing Google self-start. RecoveryOFF opened full Gemini. These are separate tests, not a claim every scenario was repeated on one final install.
- Voice recognition→route in Automotive was confirmed earlier on Tang with Google 03; Google 08 keeps that route redirection. **Route handoff was not repeated after the final54N recovery test.**
- Google 08 full reboot retained assistant/compact Live on an API 32 emulator. **Final54N car reboot, other BYD firmware, DiLink 3/5.1 Gemini, remain untested.**
- A fresh API 32 ARM64 emulator with official Morphe Manager 2.0.0 passed Local combined-bundle import, Maps APK and whole Google APKM patch/Save, shared key export, installation through Manager and ART verification. Google opens to its privacy screen. All classes/method bodies, resource contents and native libraries match the Desktop baselines semantically; DEX layout and resource paths can differ. An authenticated Gemini request passed after account setup and permissions. Compact mode and route handoff on this merged output were deferred by the owner to car testing.
- Complete Google APKM also merges and patches in Desktop, but the **merged output has not been run on the car**. The reproducible Tang path retains all 10 splits; [details](GEMINI-BUILD.en.md).

## Patch asset

`maps-automotive-for-byd-morphe-preview.6.mpp`

SHA-256: `44371318bacd9b376a4d5a1fbe4069c953e93a122725f22c7a37afd1c0c074bb`. The Morphe Remote source serves preview 6. No new ready Google/Maps APK is publicly attached. The old Telegram ready APK remains preview 2/build34 and has no Gemini feature.

[Full feature guide](README.md) · [Quick Gemini setup](GEMINI-SETUP.en.md)
