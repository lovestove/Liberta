param(
    [string]$AndroidToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip",
    [string]$GradleUrl = "https://services.gradle.org/distributions/gradle-8.13-bin.zip",
    [string]$GoUrl = "https://go.dev/dl/go1.24.2.windows-amd64.zip"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$Root = Split-Path -Parent $PSScriptRoot
$Toolchain = Join-Path $Root ".toolchain"
$Downloads = Join-Path $Toolchain "downloads"
$AndroidHome = Join-Path $Toolchain "android-sdk"
$GradleHome = Join-Path $Toolchain "gradle-8.13"
$GoHome = Join-Path $Toolchain "go"
$JavaHome = "C:\Users\Serezha\java\17.0.13+11"

New-Item -ItemType Directory -Force -Path $Downloads, $AndroidHome | Out-Null

function Download-IfMissing([string]$Url, [string]$Path) {
    if (Test-Path -LiteralPath $Path) {
        $existing = Get-Item -LiteralPath $Path
        if ($existing.Length -gt 0 -and (Test-Zip $Path)) { return }
        Remove-Item -LiteralPath $Path -Force
    }
    Write-Host "Downloading $Url"
    Invoke-WebRequest -Uri $Url -OutFile $Path -UseBasicParsing
    $downloaded = Get-Item -LiteralPath $Path
    if ($downloaded.Length -le 0) {
        throw "Downloaded file is empty: $Path"
    }
    if (!(Test-Zip $Path)) {
        Remove-Item -LiteralPath $Path -Force
        throw "Downloaded file is not a valid ZIP: $Path"
    }
}

function Test-Zip([string]$Path) {
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $zip.Dispose()
        return $true
    } catch {
        return $false
    }
}

if (!(Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) {
    throw "JDK 17 not found at $JavaHome. Install JDK 17 or edit tools\bootstrap.ps1."
}

$GradleZip = Join-Path $Downloads "gradle-8.13-bin.zip"
Download-IfMissing $GradleUrl $GradleZip
if (!(Test-Path -LiteralPath $GradleHome)) {
    Expand-Archive -LiteralPath $GradleZip -DestinationPath $Toolchain -Force
}

$ToolsZip = Join-Path $Downloads "android-commandline-tools.zip"
Download-IfMissing $AndroidToolsUrl $ToolsZip
$Latest = Join-Path $AndroidHome "cmdline-tools\latest"
if (!(Test-Path -LiteralPath (Join-Path $Latest "bin\sdkmanager.bat"))) {
    $TempTools = Join-Path $Toolchain "cmdline-tools-temp"
    if (Test-Path -LiteralPath $TempTools) { Remove-Item -LiteralPath $TempTools -Recurse -Force }
    Expand-Archive -LiteralPath $ToolsZip -DestinationPath $TempTools -Force
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Latest) | Out-Null
    Move-Item -LiteralPath (Join-Path $TempTools "cmdline-tools") -Destination $Latest -Force
    Remove-Item -LiteralPath $TempTools -Recurse -Force
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome
$SdkManager = Join-Path $Latest "bin\sdkmanager.bat"

$Licenses = Join-Path $AndroidHome "licenses"
New-Item -ItemType Directory -Force -Path $Licenses | Out-Null
@"
24333f8a63b6825ea9c5514f83c2829b004d1fee
d56f5187479451eabf01fb78af6dfcb131a6481e
8933bad161af4178b1185d1a37fbf41ea5269c55
"@ | Set-Content -LiteralPath (Join-Path $Licenses "android-sdk-license") -Encoding ASCII
@"
84831b9409646a918e30573bab4c9c91346d8abd
"@ | Set-Content -LiteralPath (Join-Path $Licenses "android-sdk-preview-license") -Encoding ASCII

& $SdkManager --sdk_root=$AndroidHome "platform-tools" "platforms;android-36" "build-tools;35.0.0" "ndk;27.0.12077973"

$GoZip = Join-Path $Downloads "go.zip"
Download-IfMissing $GoUrl $GoZip
if (!(Test-Path -LiteralPath (Join-Path $GoHome "bin\go.exe"))) {
    $GoTemp = Join-Path $Toolchain "go-temp"
    if (Test-Path -LiteralPath $GoTemp) { Remove-Item -LiteralPath $GoTemp -Recurse -Force }
    Expand-Archive -LiteralPath $GoZip -DestinationPath $GoTemp -Force
    Move-Item -LiteralPath (Join-Path $GoTemp "go") -Destination $GoHome -Force
    Remove-Item -LiteralPath $GoTemp -Recurse -Force
}

$env:PATH = "$(Join-Path $GoHome "bin");$env:PATH"
$env:GOPATH = Join-Path $Toolchain "gopath"
$env:PATH = "$(Join-Path $env:GOPATH "bin");$env:PATH"

& (Join-Path $GoHome "bin\go.exe") install golang.org/x/mobile/cmd/gomobile@latest
& (Join-Path $GoHome "bin\go.exe") install golang.org/x/mobile/cmd/gobind@latest
& (Join-Path $env:GOPATH "bin\gomobile.exe") init

@"
sdk.dir=$($AndroidHome.Replace("\", "\\"))
"@ | Set-Content -LiteralPath (Join-Path $Root "local.properties") -Encoding ASCII

Write-Host "Bootstrap complete."
Write-Host "Run: tools\build_libbox.ps1"
Write-Host "Then: tools\gradle.ps1 :app:assembleDebug"
