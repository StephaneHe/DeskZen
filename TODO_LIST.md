# DeskZen — TODO

## Done
- [x] Anti-accidental-call confirmation overlay on the landscape quick-contacts
      screen (2-step deliberate confirm + 10 s auto-cancel + tap-outside cancel). — v1.2.0
- [x] Harden "Désinstaller": try ACTION_DELETE + ACTION_UNINSTALL_PACKAGE, catch
      errors, no crash/no-op. — v1.3.2

## Next
- [ ] Real-device confirmation of the confirmation overlay (in-car / hands-free).
- [ ] Real-device confirmation of "Désinstaller" on the vivo V30T ROM (was crashing/no-op).
- [ ] Add a release `signingConfig` (currently the release APK is unsigned).
- [ ] Consider a per-contact toggle to require confirmation only for calls (currently
      all speed-dial actions are confirmed).
- [ ] Deferred cleanup: refactor the two monoliths (`LauncherScreen`, `LauncherViewModel`).
- [ ] Deferred cleanup: dependency updates (needs SDK 36 + coupled Kotlin/Compose bumps).
