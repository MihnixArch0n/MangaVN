# ═══ MyBooksLibrary Debug Script ═══
# Chạy: .\debug.ps1
# Chức năng: Build → Install → Launch → Stream logs realtime

$adb = "C:\Android\Sdk\platform-tools\adb.exe"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "═══ Building..." -ForegroundColor Cyan
.\gradlew.bat assembleDebug 2>&1 | Select-String "BUILD|error:|FAILED"

if ($LASTEXITCODE -ne 0) {
    Write-Host "═══ BUILD FAILED ═══" -ForegroundColor Red
    exit 1
}

Write-Host "═══ Installing..." -ForegroundColor Cyan
& $adb install -r app\build\outputs\apk\debug\app-debug.apk

Write-Host "═══ Launching..." -ForegroundColor Cyan
& $adb shell am force-stop com.example.mybookslibrary
Start-Sleep -Seconds 1
& $adb logcat -c
& $adb shell am start -n com.example.mybookslibrary/.MainActivity

Write-Host "═══ Streaming logs (Ctrl+C to stop)..." -ForegroundColor Green
Write-Host "Filter: KansoApp + AndroidRuntime + crashes" -ForegroundColor Yellow
Write-Host ""

# Stream realtime logs — chỉ hiện app logs + crashes
& $adb logcat -v time KansoApp:V AndroidRuntime:E *:S
