# Liberta Go Core

The MVP uses SagerNet `sing-box/experimental/libbox` as the Go core.

Build it with:

```powershell
cd C:\Projects\Liberta
.\tools\build_libbox.ps1
```

The generated Android archive is copied to:

```text
app/libs/libbox.aar
```

The Kotlin app loads libbox by reflection so the project remains inspectable before the heavy native AAR is built. Runtime connection still requires the real AAR.
