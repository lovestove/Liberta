param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$GradleBat = Join-Path $Root ".toolchain\gradle-8.13\bin\gradle.bat"
$JavaHome = "C:\Users\Serezha\java\17.0.13+11"
$AndroidHome = Join-Path $Root ".toolchain\android-sdk"

if (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe")) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$(Join-Path $JavaHome "bin");$env:PATH"
}
if (Test-Path -LiteralPath $AndroidHome) {
    $env:ANDROID_HOME = $AndroidHome
    $env:ANDROID_SDK_ROOT = $AndroidHome
}

if (!(Test-Path -LiteralPath $GradleBat)) {
    throw "Gradle 8.13 not found in .toolchain. Run tools\bootstrap.ps1 first."
}

& $GradleBat @GradleArgs
exit $LASTEXITCODE
