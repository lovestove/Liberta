# Liberta

Premium Android VPN service based on **sing-box**.

## Project Goal
Create a high-end, user-friendly, and completely free VPN application for Android that provides seamless access to the open web with stunning visual aesthetics.

## Что реализовано в MVP

- **Kotlin + Jetpack Compose + MVVM shell**: Современная база на Android.
- **VpnService foreground lifecycle**: Корректная работа в фоне.
- **Adaptive Living UI**: Интерфейс с шейдерами, параллаксом и визуализацией трафика.
- **VLESS parser & TCP server racing**: Оптимизация выбора серверов.
- **libbox.aar integration**: Использование ядра sing-box через SagerNet gomobile.
- **Phantom Call control-plane**: Мимикрия под WebRTC звонки для обхода блокировок.
- **Sovereign Mesh**: Возможность работы в качестве relay-узла.
- **GitHub release workflow**: Автоматическая сборка и публикация APK.

## Сборка

```powershell
cd C:\Projects\Liberta
.\tools\bootstrap.ps1
.\tools\build_libbox.ps1
.\tools\gradle.ps1 :app:assembleDebug
```

Подробный запуск и проверка: `docs\RUNBOOK.md`.

## Документация
- [SPEC_DRIVEN.md](SPEC_DRIVEN.md) - полная спецификация проекта.
- [RUNBOOK.md](docs/RUNBOOK.md) - руководство по сборке и запуску.
