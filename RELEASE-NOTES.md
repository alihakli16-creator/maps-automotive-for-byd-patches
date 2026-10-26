# Morphe preview 9 — Send to car (TEST)

**26 October 2026 · Maps55E + optional Google20 (unchanged) · own patches/code only.** One `.mpp`, two app-specific patches. No Google APKs are distributed.

## Changes since preview 8

- **Send to car from the phone:** in Google Maps on your phone choose a route → **Send to car** → your car (listed as **byd (xxxx)**). The route opens in Maps on the head unit as a route preview with **Start**, usually within a few seconds. Use the same Google account on the phone and in Maps.
- **Instrument cluster → Send to car → Keep phone routes arriving (new):** turn it on once. Maps uses its local ADB key (the same key as wheel buttons, Gemini recovery and the battery reader; approve **Always allow** if asked) to exempt MicroG-RE from battery optimization, then asks MicroG-RE to reconnect. Without the exemption, Android 12 stops MicroG-RE from restarting its push connection after the car is switched on, and routes do not arrive until it reconnects. **Reconnect now** forces a reconnect. Turning the switch off removes the exemption.
- **What makes it work with MicroG-RE:**
  - Maps push is routed to MicroG-RE's service names, so the car can register for Google's notifications.
  - Notification registration uses the same app identity that MicroG-RE uses for sign-in. With the installed Automotive package name, Google rejected the registration (`INVALID_ARGUMENT`) and the car never appeared on the phone. YouTube had the same problem ([Morphe Patches #3481](https://github.com/MorpheApp/morphe-patches/pull/3481)).
  - Routes arrive as `maps.app.goo.gl` links. Maps normally expands them through Play services' Dynamic Links service, which fails in MicroG-RE ([MicroG-RE #329](https://github.com/MorpheApp/MicroG-RE/issues/329)), so the route was dropped without a message. Maps now follows the link's HTTPS redirect itself (Google Maps addresses only). A freshly sent link can answer 404 for a few seconds, so Maps retries after 1, 2 and 4 seconds.
- **Google patch unchanged (Google20).** A Google app built from preview 8 is byte-identical to one built from preview 9.

## Install / update

Refresh the Morphe source until it shows **preview9**, or import this release's `.mpp` as Local. Disable older duplicate sources for these patches. **Repatch Maps** from the same original Maps Automotive **26.10.120001.E /1068833407**, sign with **the same personal key** and install as an update; no data reset is needed. Rebuild the Google app only if you come from preview 7 or earlier. Then open the cluster settings → **Send to car** and turn on **Keep phone routes arriving**. MicroG-RE **7.2.2+** is required; get Morphe and MicroG-RE from their official GitHub Releases. [Full guide](START-HERE.en.md#9-send-to-car-from-your-phone-optional).

## Verification limits

Tested on one BYD Tang (DiLink 5.0, Android 12) with MicroG-RE 7.2.2. The owner patched the test build 55E in Morphe Manager; its Maps output is byte-identical to this release. The car appeared on the phone as **byd**, three routes sent in a row all opened (two of them after the automatic retry), and the switch showed ON. The same chain was first reproduced on an Android 12 (API 32) emulator signed in to the owner's account. Not yet tested: DiLink 3 and DiLink 5.1 cars, other MicroG-RE versions, and a car restart after enabling the exemption from the new switch (on the test car the same command was run once by hand). Internally Maps delivers each route to itself twice; no visible effect was seen. Live-to-Maps directions remain unverified, as in preview 8.

The release is rebuilt from our own sources with 593 host checks (96 Maps recovery + 45 short-link + 27 Send to car + 425 Google policy/focus lifecycle). Its Maps output matches the tested 55E (3265 entries, 0 differences) and its Google output matches preview 8 from the same input (14020 entries, 0 differences). Both originals must pass the pinned version/signature/fingerprint checks. Public asset verification is recorded in `VERIFICATION.json`. This remains a TEST prerelease.
