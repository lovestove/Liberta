param(
    [string]$SingBoxVersion = "v1.13.11"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Toolchain = Join-Path $Root ".toolchain"
$AndroidHome = Join-Path $Toolchain "android-sdk"
$GoHome = Join-Path $Toolchain "go"
$GoPath = Join-Path $Toolchain "gopath"
$Gomobile = Join-Path $GoPath "bin\gomobile.exe"
$Out = Join-Path $Root "app\libs\libbox.aar"

$env:JAVA_HOME = "C:\Users\Serezha\java\17.0.13+11"
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome
$env:ANDROID_NDK_HOME = Join-Path $AndroidHome "ndk\27.0.12077973"
$env:ANDROID_NDK_ROOT = $env:ANDROID_NDK_HOME
$env:GOPATH = $GoPath
$env:PATH = "$(Join-Path $env:JAVA_HOME "bin");$(Join-Path $GoHome "bin");$(Join-Path $GoPath "bin");$env:PATH"

if (!(Test-Path -LiteralPath $Gomobile)) {
    throw "gomobile not found. Run tools\bootstrap.ps1 first."
}
if (!(Test-Path -LiteralPath $env:ANDROID_NDK_HOME)) {
    throw "Android NDK not found. Run tools\bootstrap.ps1 first."
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Out) | Out-Null

Push-Location (Join-Path $Root "core\go")
& (Join-Path $GoHome "bin\go.exe") get "github.com/sagernet/sing-box@$SingBoxVersion"
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
& (Join-Path $GoHome "bin\go.exe") get "golang.org/x/mobile@v0.0.0-20260410095206-2cfb76559b7b"
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
& (Join-Path $GoHome "bin\go.exe") mod tidy
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
& $Gomobile bind `
    -v `
    -target=android `
    -androidapi=23 `
    -tags="with_gvisor,with_quic,with_utls,with_clash_api" `
    -ldflags="-checklinkname=0" `
    -o $Out `
    "github.com/sagernet/sing-box/experimental/libbox"
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host "Created $Out"
