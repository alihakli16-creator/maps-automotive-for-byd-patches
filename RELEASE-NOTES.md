# Morphe preview 7 — cluster pointer, wheel buttons and Gemini fixes (TEST)

**Published 23 October 2026 · TEST preview.** One bundle contains our Maps 54R and optional Google 08 patches. GitHub distributes own patches/code/docs only; no Google APK. This is a test preview.

## Changes since preview 6

- **Horizontal pointer offset.** Instrument cluster → Map layout → **Pointer horizontal offset** (−25…25 %, 0 = standard centre) moves the car pointer left or right on the cluster map, without a route and during navigation. **Reset pointer position** returns to vertical 10 %, horizontal 0 %.
- **Steering-wheel button assignment.** Steering-wheel zoom → **Custom / Zoom in / Zoom out button**, and Gemini voice assistant → **Gemini button**: tap the row, then press the wheel button within 10 seconds. Maps saves its key code, scan code and input device; that press is not passed to the car. BYD Tang buttons stay the defaults; **Restore** rows bring them back. One button cannot be both a zoom and the Gemini button.
- **Recent keys** on both pages: the last 20 keys this head unit sends to Android, and whether Maps kept or passed each one. A wheel button that never appears there is handled by the head unit itself and cannot be assigned.
- **Gemini after switching the car on.** On the tested Tang, Google's voice service sometimes stayed half connected after the car was switched off and on: a tap showed only the screen-edge glow, and Google reported that the invocation source was not enabled. With **Automatic compact recovery** ON, Maps now checks 2.5 seconds after a tap whether a Gemini window opened. If not, and Google's service is still not connected two seconds later, Maps re-registers only Google as the assistant through the approved local ADB connection and retries once. If the service is already connected, nothing is changed or retried. Another selected assistant is never changed.
- **A second press closes compact Gemini**, on the wheel or on the map, instead of opening the full Gemini app.
- The Google app patch (08) is unchanged; a working patched Google app can stay installed.

## Installation

Same originals as preview 6: Maps Automotive **26.10.120001.E / 1068833407**; for Gemini the pinned Google **17.62.11.ve.arm64 / 301813762 API 32+ APKM**. Sign with your **same backed-up key**; same-key Maps updates keep data. In Morphe Manager, refresh the Remote source until it shows preview 7, or import the `.mpp` as a Local source. **Turn off older sources:** all previews use the same patch name, and on the tested car Manager once applied the older preview 6 source instead of the new Local file. [Short Gemini setup](GEMINI-SETUP.en.md) · [Exact inputs/build](GEMINI-BUILD.en.md) · [Full guide](README.md).

Button assignment and the second-press close use the existing wheel-key service, so **Steering-wheel zoom** must be enabled and **Connected**. Recovery still needs BYD **General → Apps → Disable Self-Start → Google = OFF**, Maps **Compact assistant ON**, **Automatic compact recovery ON**, and the Maps ADB key approved with **Always allow**.

## Verification

- Rebuilt from our own sources. The Maps extension DEX is byte-identical to the tested build 54R; the Google extension DEX and the patch DEX are byte-identical to preview 6. Applied with Desktop 1.18.1: the Maps output matches build 54R in all 31 310 classes and all 3258 non-signature ZIP entries. Our independent DEX comparison with the original Maps found only the 70 expected hooks and 0 unexpected changes, and the register-flow check passed.
- Host tests: 71 recovery-protocol checks (unchanged from preview 6), 25 new stalled-recovery checks and 301 Google provider/routing checks run during every build. Further host tests cover the horizontal pointer (151 plus the 74 earlier checks with horizontal offset 0), the navigation-camera hook on the original DEX (50), all readers of the pointer position (10), button assignment and recent keys (41), the Gemini button (14, plus the unchanged earlier 35), and the earlier wheel-service suites.
- On one **BYD Tang (DiLink 5.0, Android API 32)**, the owner installed test builds 54O, 54P and 54Q with Morphe Manager as updates that kept app data, accounts and the signing key. The installed code matched each build exactly. Maps did not crash, the wheel-key service stayed connected, and compact Gemini opened from Maps.
- On the same car, after the car was switched off and on, Google's voice service stayed half connected and Gemini showed only the glow. Toggling Google in Disable Self-Start did not help that time. Re-registering only Google as the assistant, the same step the new recovery performs, connected the service within 2 seconds, and compact Gemini opened.
- **Not yet tested in the car:** the automatic stalled recovery itself (it needs the next car start), closing Gemini with a second press (build 54R), the horizontal pointer and the button assignment on a real steering wheel. Other BYD models and firmware are not tested.

## Patch asset

`maps-automotive-for-byd-morphe-preview.7.mpp`

SHA-256: `241da5726592d5fc3847c676b54349c31b73320cb28ab2040d6cfb52e83f6a01`. The Morphe Remote source serves preview 7. No ready Maps or Google APK is attached. The old Telegram ready APK remains preview 2/build 34 and has none of these features.

[Full feature guide](README.md) · [Quick Gemini setup](GEMINI-SETUP.en.md)
