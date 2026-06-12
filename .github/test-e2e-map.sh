#!/bin/sh
# Test local cho logic selective e2e (mô phỏng đúng đoạn bash trong ci.yml job changes).
# Chạy: sh .github/test-e2e-map.sh — PASS khi cả 4 kịch bản đúng kỳ vọng.
MAP="$(dirname "$0")/e2e-test-map.json"
FAIL=0

run_filter() {
    printf '%s\n' "$1" > /tmp/e2e-map-changed.txt
    NOTCLASS=""
    # Local dùng py thay jq (Git Bash Windows không có jq; CI Ubuntu dùng jq)
    GLOBAL=$(py -c "import json,sys;print(json.load(open(sys.argv[1],encoding='utf-8'))['globalPaths'])" "$MAP")
    if ! grep -Eq "$GLOBAL" /tmp/e2e-map-changed.txt; then
        TSV=$(py -c "import json,sys;[print(f['testClass']+'	'+f['paths']) for f in json.load(open(sys.argv[1],encoding='utf-8'))['features']]" "$MAP")
        OLD_IFS=$IFS
        IFS='
'
        for LINE in $TSV; do
            CLASS=$(printf '%s' "$LINE" | cut -f1)
            PATHS_RE=$(printf '%s' "$LINE" | cut -f2)
            if ! grep -Eq "$PATHS_RE" /tmp/e2e-map-changed.txt; then
                NOTCLASS="${NOTCLASS:+$NOTCLASS,}$CLASS"
            fi
        done
        IFS=$OLD_IFS
    fi
    echo "$NOTCLASS"
}

check() {
    DESC=$1; CHANGED=$2; EXPECTED=$3
    GOT=$(run_filter "$CHANGED")
    if [ "$GOT" = "$EXPECTED" ]; then
        echo "OK   $DESC -> notclass='$GOT'"
    else
        echo "FAIL $DESC -> notclass='$GOT' (expected '$EXPECTED')"
        FAIL=1
    fi
}

DL_TEST="com.example.mybookslibrary.data.download.DownloadFlowTest"

# PR chỉ đụng UI Library -> exclude DownloadFlowTest
check "UI-only PR" "app/src/main/java/com/example/mybookslibrary/ui/screens/LibraryScreen.kt" "$DL_TEST"
# PR đụng download -> chạy đủ
check "download PR" "app/src/main/java/com/example/mybookslibrary/data/download/OfflineDownloadStorage.kt" ""
# PR đụng build files (globalPaths) -> chạy đủ
check "build-files PR" "app/build.gradle.kts" ""
# PR đụng DI (globalPaths) -> chạy đủ
check "DI PR" "app/src/main/java/com/example/mybookslibrary/di/AppModule.kt" ""
# PR đụng data/local (DAO — download paths) -> chạy đủ
check "DAO PR" "app/src/main/java/com/example/mybookslibrary/data/local/dao/ChapterDao.kt" ""

[ $FAIL -eq 0 ] && echo "PASS" || echo "CÓ CASE FAIL"
exit $FAIL
