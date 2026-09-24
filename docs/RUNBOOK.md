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

Главная кнопка `Мимикрия под звонки` и Labs -> `Мимикрия под звонки` включают клиентский control-plane. Этот режим не должен считаться успешным из-за прямого подключения к обычному серверу:

1. Liberta создает комнату звонка или использует пользовательскую ссылку.
2. Если указан `Bridge URL звонка`, выполняется сигналинг `join/leave`.
3. Если bridge возвращает VLESS-ссылку, приложение использует ее как канал, полученный через звонок.
4. Если bridge отсутствует, UI честно сообщает, что звонок создан, но media-runtime для передачи VPN через звонок не подключен.

Для обычного VPN без звонка используйте `Черные списки` или `Белые списки`.

## Encrypted Stego Images

Workflow `.github/workflows/subscription-stego.yml` раз в час с задержкой 0-10 минут запускает `tools/stego_pack.py`, скачивает белую и черную подписки, шифрует snapshot в PNG и публикует artifact `liberta-stego-images`.

Локальная проверка упаковщика:

```powershell
python -m pip install cryptography
python tools\stego_pack.py --out build\stego
```

Для production задайте GitHub Secret `LIBERTA_STEGO_KEY`. VK-отправка пока заготовлена как stub; реальные `VK_ACCESS_TOKEN` и `VK_GROUP_ID` будут использоваться в этом же workflow после подключения API.

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
