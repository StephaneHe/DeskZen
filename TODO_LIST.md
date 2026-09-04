# DeskZen — TODO

## Done
- [x] Anti-accidental-call confirmation overlay on the landscape quick-contacts
      screen (2-step deliberate confirm + 10 s auto-cancel + tap-outside cancel). — v1.2.0

## Next
- [ ] Real-device confirmation of the confirmation overlay (in-car / hands-free).
- [ ] Add a release `signingConfig` (currently the release APK is unsigned).
- [ ] Consider a per-contact toggle to require confirmation only for calls (currently
      all speed-dial actions are confirmed).
- [ ] Deferred cleanup: refactor the two monoliths (`LauncherScreen`, `LauncherViewModel`).
- [ ] Deferred cleanup: dependency updates (needs SDK 36 + coupled Kotlin/Compose bumps).
