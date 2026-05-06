# Liberta

Новый Android-only MVP, собранный по `SPEC_DRIVEN.md`.

## Что реализовано в MVP

- Kotlin + Jetpack Compose + MVVM shell.
- `VpnService` foreground lifecycle.
- Загрузка двух подписок из `SPEC_DRIVEN.md`.
- VLESS parser, TCP server racing, sing-box config builder.
- `libbox.aar` integration point через SagerNet `gomobile`.
- DNS для Android TUN: система получает реальный IP DNS из настроек, sing-box перехватывает DNS через `hijack-dns`, upstream DNS идет через `direct`.
- Crystal Lens UI: светлый физичный фон, центральная линза включения между заголовком и нижней floating-панелью, отдельный diffraction/dispersion shader для линзы, стеклянный знак питания внутри нее и линзовая поверхность у каждой кнопки.
- DataStore settings: способ подключения, быстрый auto-refresh interval от 5 минут, DNS, MTU, IPv6, kill switch, proxy, split tunneling, UI comfort, expanded Russian Labs.
- Phantom Call control-plane: auto-mode создает бесплатную Jitsi-комнату без ручного Bridge URL; пользовательский bridge по-прежнему поддерживается как расширенный путь.
- Mesh access и режим помощи разделены: кнопка `Меш-сеть` подключает пользователя к mesh-доступу, а благодарность `Вы помогаете людям, спасибо` появляется только при включении себя как relay-узла в Labs.
- GitHub release workflow: теги `v*` собирают APK и публикуют `liberta.apk` в GitHub Releases.

## Сборка

```powershell
cd C:\Projects\Liberta
.\tools\bootstrap.ps1
.\tools\build_libbox.ps1
.\tools\gradle.ps1 :app:assembleDebug
```

Подробный запуск и проверка: `docs\RUNBOOK.md`.

Debug APK: `app\build\outputs\apk\debug\app-debug.apk`.

Публичная ссылка для QR/share: `https://github.com/lovestove/Liberta`.
