# Liberta Runbook

Рабочая папка проекта: `C:\Projects\Liberta`.

## Первый запуск

```powershell
cd C:\Projects\Liberta
.\tools\bootstrap.ps1
.\tools\build_libbox.ps1
.\tools\gradle.ps1 testDebugUnitTest
.\tools\gradle.ps1 :app:assembleDebug
```

APK появится в `app\build\outputs\apk\debug\app-debug.apk`.

`tools\build_libbox.ps1` собирает AAR с тегами `with_gvisor,with_quic,with_utls,with_clash_api`. `with_clash_api` нужен реальному `CommandServer.start()` в libbox v1.13.x.

## GitHub Release

Workflow `.github/workflows/release-apk.yml` запускается на тегах `v*` и публикует `liberta.apk` в GitHub Releases:

```powershell
git tag v0.1.1
git push origin v0.1.1
```

Локальная папка `C:\Projects\Liberta` сейчас не является git checkout, поэтому публикация релиза возможна только после переноса этих файлов в репозиторий `https://github.com/lovestove/Liberta`.

## Phantom Call

Главная кнопка `Мимикрия под звонки` и Labs -> `Мимикрия под звонки` включают клиентский control-plane:

1. Если пользователь не указал bridge, Liberta автоматически создает бесплатную комнату `meet.jit.si` и не требует ручной настройки.
2. Если пользовательский bridge указан в сохраненных настройках, выполняется сигналинг bridge и cleanup при отключении.
3. Базовый VPN-туннель не должен падать только из-за отсутствия ручного bridge.

Liberta не показывает успешный WebRTC media tunnel без совместимого bridge/media runtime; auto-mode нужен для бесплатной мимикрии control-plane и не блокирует основной VLESS VPN.

## Проверка на телефоне

```powershell
$env:ANDROID_HOME="C:\Projects\Liberta\.toolchain\android-sdk"
.\.toolchain\android-sdk\platform-tools\adb.exe install -r .\app\build\outputs\apk\debug\app-debug.apk
.\.toolchain\android-sdk\platform-tools\adb.exe shell am start -n com.liberta.vpn/.MainActivity
.\.toolchain\android-sdk\platform-tools\adb.exe logcat -s LibertaCore LibertaVpnService AndroidRuntime
```

Статус `CONNECTED` / `Работоспособность подтверждена` в UI появляется только после всех условий:

1. Android выдал `VpnService` TUN.
2. Android получил реальный IP DNS из настроек, а sing-box перехватил DNS через `hijack-dns`.
3. `libbox.CheckConfig` принял сгенерированный sing-box JSON.
4. `libbox` запустил service и post-connect internet probe прошел.

Если TUN поднялся, но internet probe не прошел, сервис закрывает текущий core и пробует следующий сервер. Он не оставляет VPN в состоянии "рабочий", пока интернет не подтвержден.

Кнопка `Меш-сеть` означает подключение пользователя к mesh-доступу. Она не включает режим помощи другим; фраза `Вы помогаете людям, спасибо` появляется только после отдельного включения relay в Labs.

Если `libbox.aar` отсутствует, приложение не имитирует подключение и покажет ошибку.

## Финальная локальная проверка

```powershell
.\tools\gradle.ps1 testDebugUnitTest
.\tools\gradle.ps1 :app:assembleDebug
.\.toolchain\android-sdk\build-tools\35.0.0\apksigner.bat verify --verbose .\app\build\outputs\apk\debug\app-debug.apk
.\.toolchain\android-sdk\build-tools\35.0.0\aapt.exe dump badging .\app\build\outputs\apk\debug\app-debug.apk
```

Если телефон подключен, после установки проверьте запуск UI, QR/share на `https://github.com/lovestove/Liberta`, настройки Labs, подключение black profile до `CONNECTED` после health probe и пустой crash-buffer. Если `adb devices` пустой, device-loop не выполняется.
