# Liberta MVP Review

## Architecture

- Source of truth: `SPEC_DRIVEN.md`.
- App package: `com.liberta.vpn`.
- UI: Compose-only Crystal Lens UI with a physical diffraction power lens, lens-shaped power glyph, lens surfaces for every button, bottom floating connection dock, GitHub QR/share overlay, and expandable Russian Labs settings.
- System boundary: Android `VpnService`.
- Core boundary: `CoreEngine` with `libbox` reflection adapter; no fake connected state.
- Runtime bridge: `Libbox.setup`, `newCommandServer`, `CommandServer.startOrReloadService`.
- DNS boundary: Android receives a real configured DNS IP as TUN DNS; sing-box route uses `hijack-dns`.
- Runtime truth boundary: `CONNECTED` is emitted only after post-start internet validation. Failed probes close the current core and move to the next candidate instead of presenting a degraded tunnel as working.
- Mesh boundary: `Меш-сеть` is a connection method; `sovereignRelay` remains the separate "help others" switch that controls the gratitude ribbon and help statistics.

## Risk Register

- `libbox.aar` must be built before a real VPN session can connect; v1.13.x requires `with_clash_api`.
- Advanced Labs are persisted and represented in UI. `Polymorphic DNA` still affects generated config opportunistically; `Мимикрия под звонки` now has auto free-room control-plane and optional bridge cleanup, but no fake media tunnel success path.
- The GitHub release workflow publishes `liberta.apk` on `v*` tags, but this local folder is not a git repo; release automation only runs after changes are pushed to `lovestove/Liberta`.
- Public subscriptions can be empty, blocked, base64 encoded, CIDR-only, or contain dead endpoints; cache, white-list fallback, spread-out racing, and strict post-start probes improve this but cannot guarantee live public servers.
- Shell tools on this ROM do not reliably use Android netd DNS, so UI health probe and ConnectivityService validation are the authoritative device checks.

## Validation Gates

- `tools\bootstrap.ps1`
- `tools\build_libbox.ps1`
- `tools\gradle.ps1 testDebugUnitTest`
- `tools\gradle.ps1 :app:assembleDebug`
- `apksigner verify --verbose app\build\outputs\apk\debug\app-debug.apk`
- `aapt dump badging app\build\outputs\apk\debug\app-debug.apk`
- Current local validation: unit tests, debug APK build, `apksigner verify`, and `aapt dump badging`. Device install requires an attached phone; `adb devices` should list a device before running the phone loop.
