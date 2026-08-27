# Changelog

All notable changes to DeskZen will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2026-04-29

### Fixed
- Long-press sur une cellule vide du home screen : ouvre désormais un menu contextuel à deux entrées (« Créer un raccourci web » / « Changer le fond d'écran ») via `EmptyCellActionDialog`. Régression introduite par l'ajout de l'accès direct au wallpaper qui écrasait l'ancien handler raccourci web.

## [1.0.1] - 2026-04-29

### Added
- Visible version footer (`DeskZen vX.Y.Z`) at the bottom of `FolderManagerSheet`, read from `BuildConfig.VERSION_NAME`.
- Ambient mode on unlock: home screen icons fade to ~6% opacity on `ON_RESUME`; double-tap toggles back to full opacity with a 180ms tween. Wallpaper dim overlay animates between 35% and 52%.
- Backup/restore now includes contact photos (Base64-encoded in the JSON, decoded back to `filesDir` on import).

### Changed
- Initial `versionCode` bump from `1` to `2` and `versionName` from `1.0.0` to `1.0.1` to comply with the fleet versioning rule.
- Pager fluidity: replaced `detectVerticalDragGestures` with a custom horizontal-yielding gesture; added `beyondViewportPageCount = 1` to pre-render adjacent pages.
- Portrait/landscape orientation switch wrapped in `AnimatedContent` with `fadeIn() togetherWith fadeOut()` for a smooth crossfade instead of an instant snap.

## [1.0.0] - 2026-04-28

### Added
- Initial release.
- Home screen with paginated grid (`HorizontalPager`), drag & drop, dock, app drawer.
- IA-driven folder categorization (`HeuristicCategorizer`).
- Web shortcuts with favicon fetching.
- Landscape mode: 4×2 quick-contacts grid with phone call, WhatsApp call/message, and SMS actions.
- Custom wallpaper picker.
- Backup/restore via JSON file in `filesDir`.
- Notification badge counts.
- Double-tap Home to scroll back to the first page.
