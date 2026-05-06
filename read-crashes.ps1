# ═══ Đọc crash logs từ device ═══
# Chạy: .\read-crashes.ps1

$adb = "C:\Android\Sdk\platform-tools\adb.exe"

Write-Host "═══ Crash logs trên device ═══" -ForegroundColor Cyan
$files = & $adb shell ls /data/data/com.example.mybookslibrary/cache/crash_*.txt 2>&1

if ($files -match "No such file") {
    Write-Host "Không có crash log nào." -ForegroundColor Green
} else {
    $files -split "`n" | ForEach-Object {
        $f = $_.Trim()
        if ($f -and $f -match "crash_") {
            Write-Host "`n═══ $f ═══" -ForegroundColor Yellow
            & $adb shell cat $f
        }
    }
}

Write-Host "`n═══ Recent logcat crashes ═══" -ForegroundColor Cyan
& $adb logcat -d -t 200 -s KansoApp:V AndroidRuntime:E
