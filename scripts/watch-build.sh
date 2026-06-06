#!/bin/bash
# Auto rebuild + install khi có thay đổi file .kt / .xml / .gradle.kts
#
# Dùng:
#   bash scripts/watch-build.sh          # bắt đầu (foreground)
#   bash scripts/watch-build.sh start    # bắt đầu (background)
#   bash scripts/watch-build.sh stop     # dừng
#   bash scripts/watch-build.sh pause    # tạm dừng (không dừng process)
#   bash scripts/watch-build.sh resume   # tiếp tục sau pause

PIDFILE="/tmp/watch-build.pid"
DISABLE_FLAG="/tmp/auto-build.disabled"
LOGFILE="/tmp/watch-build.log"
GRADLEW="./gradlew"
ADB="C:/Android/Sdk/platform-tools/adb.exe"
APK="app/build/outputs/apk/debug/app-debug.apk"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.10"

case "${1:-}" in
    stop)
        if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
            kill "$(cat "$PIDFILE")" && rm -f "$PIDFILE"
            echo ">>> watch-build: đã dừng"
        else
            echo ">>> watch-build: không có process đang chạy"
        fi
        exit 0
        ;;
    pause)
        touch "$DISABLE_FLAG"
        echo ">>> watch-build: đã tạm dừng (resume để tiếp tục)"
        exit 0
        ;;
    resume)
        rm -f "$DISABLE_FLAG"
        echo ">>> watch-build: đã tiếp tục"
        exit 0
        ;;
    start)
        if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
            echo ">>> watch-build: đang chạy (PID $(cat "$PIDFILE"))"
            exit 0
        fi
        nohup bash "$0" >"$LOGFILE" 2>&1 &
        echo $! >"$PIDFILE"
        echo ">>> watch-build: đã khởi động (PID $!), log: $LOGFILE"
        exit 0
        ;;
esac

# --- Foreground watch loop ---
build_and_install() {
    [ -f "$DISABLE_FLAG" ] && return
    echo ""
    echo "[$(date '+%H:%M:%S')] Phát hiện thay đổi — building..."
    "$GRADLEW" assembleDebug --console=plain -q 2>&1 | tail -3
    if [ $? -eq 0 ]; then
        echo "[$(date '+%H:%M:%S')] Build OK — installing..."
        # Dùng PowerShell để tránh Git Bash convert /data/local/tmp thành Windows path
        powershell.exe -NoProfile -Command "
            \$adb = 'C:\Android\Sdk\platform-tools\adb.exe'
            \$apk = '$(pwd -W)\\app\\build\\outputs\\apk\\debug\\app-debug.apk'
            & \$adb push \$apk '/data/local/tmp/app.apk' | Out-Null
            \$result = & \$adb shell 'pm install -r /data/local/tmp/app.apk'
            if (\$result -match 'Success') { Write-Host 'OK' } else { Write-Host 'FAILED'; Write-Host \$result }
        " && echo "[$(date '+%H:%M:%S')] Install OK ✓" || echo "[$(date '+%H:%M:%S')] Install FAILED"
    else
        echo "[$(date '+%H:%M:%S')] Build FAILED"
    fi
}

echo ">>> Watching app/src/**/*.{kt,xml} và *.gradle.kts"
echo ">>> Dừng: bash scripts/watch-build.sh stop"
echo ">>> Tạm dừng/tiếp tục: pause / resume"

# Build ngay lần đầu
build_and_install

# Watch bằng PowerShell FileSystemWatcher (native Windows, không cần inotifywait)
powershell.exe -NoProfile -Command "
\$watcher = New-Object System.IO.FileSystemWatcher
\$watcher.Path = '$(pwd)/app/src'
\$watcher.Filter = '*.*'
\$watcher.IncludeSubdirectories = \$true
\$watcher.EnableRaisingEvents = \$true

Write-Host 'FileSystemWatcher ready'
while (\$true) {
    \$result = \$watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, 2000)
    if (-not \$result.TimedOut -and \$result.Name -match '\.(kt|xml|kts)$') {
        Write-Host \$result.Name
    }
}
" 2>/dev/null | while read -r changed_file; do
    [ -n "$changed_file" ] && build_and_install
done
