# Liberta MVP Review

## Architecture

- Source of truth: `SPEC_DRIVEN.md`.
- App package: `com.liberta.vpn`.
- UI: Compose-only Crystal Lens UI with a physical diffraction power lens, lens-shaped power glyph, lens surfaces for every button, bottom floating connection dock, GitHub QR/share overlay, and expandable Russian Labs settings.
- System boundary: Android `VpnService`.
- Core boundary: `CoreEngine` with `libbox` reflection adapter; no fake connected state.
- Runtime bridge: `Libbox.setup`, `newCommandServer`, `CommandServer.startOrReloadService`.
- DNS boundary: Android receives a real configured DNS IP as TUN DNS; sing-box route uses `hijack-dns`.
- Runtime truth boundary: libbox runs in the app process, so the app UID is excluded from the VPN to avoid looping the core's own VLESS sockets back into TUN. In-process probes are treated only as process-network probes; real tunnel health must be checked through traffic from an included UID or Android VPN network state.
- Mesh boundary: `Меш-сеть` is a connection method; `sovereignRelay` remains the separate "help others" switch that controls the gratitude ribbon and help statistics.

## Risk Register

- `libbox.aar` must be built before a real VPN session can connect; v1.13.x requires `with_clash_api`.
- Advanced Labs are persisted and represented in UI. `Polymorphic DNA` still affects generated config opportunistically; `Мимикрия под звонки` now has auto free-room control-plane and optional bridge cleanup, but no fake media tunnel success path.
- The GitHub release workflow publishes `liberta.apk` on `v*` tags, but this local folder is not a git repo; release automation only runs after changes are pushed to `lovestove/Liberta`.
- Public subscriptions can be empty, blocked, base64 encoded, CIDR-only, or contain dead endpoints; cache, white-list fallback and spread-out racing improve this but cannot guarantee live public servers.
- Shell traffic from `adb shell` is useful as an included-UID tunnel probe on this ROM; compare it with `dumpsys connectivity` because the app process itself is intentionally outside VPN routing.

## Validation Gates

- `tools\bootstrap.ps1`
- `tools\build_libbox.ps1`
- `tools\gradle.ps1 testDebugUnitTest`
- `tools\gradle.ps1 :app:assembleDebug`
- `apksigner verify --verbose app\build\outputs\apk\debug\app-debug.apk`
- `aapt dump badging app\build\outputs\apk\debug\app-debug.apk`
- Current local validation: unit tests, debug APK build, `apksigner verify`, and `aapt dump badging`. Device install requires an attached phone; `adb devices` should list a device before running the phone loop.
