# Morphe preview 8 — compact Gemini Live (TEST)

**25 October 2026 · Maps54T + optional Google20 · own patches/code only.** One `.mpp`, two app-specific patches. No Google APKs are distributed.

## Changes since preview 7

- **Compact Gemini Live capsule:** a small Live indicator with microphone pause/resume and close controls above Maps. It uses the existing native Google Live conversation, not the separate automotive carassistant app. The old panel no longer flashes before the capsule.
- **Live from the map and steering wheel:** our controls always request Conversation / Gemini Live. The ordinary Microphone launch mode and its settings selector have been removed. The existing button assignment, cluster zoom, pointer offsets and compact recovery remain.
- **Cluster settings beside Search:** direct access next to the search bar, including the previous54S placement change.
- **Experimental music attenuation:** Google20 requests transient audio focus while a matching visible Live session listens/responds, releasing it on pause/close. **It did NOT lower Spotify volume on our Tang/DiLink5.0.** Google returned GRANTED, but Spotify kept its focus and volume. Do not treat music ducking as a working feature on every head unit. Restoration after attenuation is also unverified because no attenuation occurred in that test.

Please test your player while parked: play music → open Live → close Live. Report your BYD model, DiLink/Android version, player name, whether music becomes quieter or pauses, and whether its volume/playback returns. Do not send account details or voice recordings.

## Install / update

Refresh the Morphe source until it shows **preview8**, or import this release's `.mpp` as Local. Disable older duplicate sources for these patches. **Repatch both apps** to get the new capsule: Maps Automotive **26.10.120001.E /1068833407** and Google **17.62.11.ve.arm64 /301813762 /API32+ APKM**. Apply only the matching patch to each original; keep **the same existing personal key for both apps** and install as updates. No Maps data reset is needed. If you already tested54T+Google20, the release has the same runtime payload, so no functional update is required.

Google/Gemini is optional; Maps-only API29 support does not make Gemini compatible with API29. MicroG-RE **7.2.2+**, separate account setup and the original Gemini shell are needed. Get Morphe and MicroG-RE from their official GitHub Releases. Keep BYD **General → Apps → Disable Self-Start → Google OFF**, Maps **Compact assistant ON**, **Automatic compact recovery ON**, and local ADB / the Maps key authorized. The existing permissions and Google assistant selection still apply. [Quick setup](GEMINI-SETUP.en.md) · [Full guide](START-HERE.en.md) · [Exact inputs](GEMINI-BUILD.en.md).

**Live directions limitation:** do not transfer the earlier ordinary Gemini route-handoff result to Live. Google currently documents directions as unavailable in Live chats ([Google help](https://support.google.com/gemini/answer/16622866)). This release does not establish voice-to-Maps route handoff from Live.

## Verification limits

The owner built/installed54T+19 via Morphe and confirmed the set worked. Google20 was then installed through Morphe on Tang; the Live capsule reached native LISTENING, its focus request reported GRANTED, and owner close released the request. Spotify attenuation failed in two manual cycles. Actual Google20 installed revision/SHA was verified. No new reboot, call-interruption, other-player/firmware or Live route test is claimed. Earlier emulator capsule pause/resume/close and a synthetic spoken answer were verified on the same capsule implementation; these are not a new real-car voice or route test.

The release is rebuilt from our own sources with521 host checks (96 Maps recovery +425 Google policy/focus lifecycle), then checked against the exact54T/20 runtime and independent original-class preservation. Both originals must pass pinned version/signature/fingerprint checks. Public asset verification is recorded in `VERIFICATION.json`. This remains a TEST prerelease.
