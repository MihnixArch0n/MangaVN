#!/bin/sh
# Gate: values/strings.xml và values-vi/strings.xml phải có CÙNG tập key —
# không để ngôn ngữ nào rơi về fallback thiếu bản dịch (refactor-ui-ux.md §8).
# Chạy ở pre-commit hook; CI có thể gọi trực tiếp.
# Exit 0 = parity OK; exit 1 = lệch key (in danh sách).

EN_FILE="app/src/main/res/values/strings.xml"
VI_FILE="app/src/main/res/values-vi/strings.xml"

if [ ! -f "$EN_FILE" ] || [ ! -f "$VI_FILE" ]; then
    echo "❌ Thiếu file strings: $EN_FILE hoặc $VI_FILE"
    exit 1
fi

TMP_EN=$(mktemp)
TMP_VI=$(mktemp)
trap 'rm -f "$TMP_EN" "$TMP_VI"' EXIT

# Lấy danh sách key (name="...") đã sort — comm yêu cầu input sorted
grep -o 'name="[^"]*"' "$EN_FILE" | sed 's/^name="//;s/"$//' | sort > "$TMP_EN"
grep -o 'name="[^"]*"' "$VI_FILE" | sed 's/^name="//;s/"$//' | sort > "$TMP_VI"

MISSING_IN_VI=$(comm -23 "$TMP_EN" "$TMP_VI")
MISSING_IN_EN=$(comm -13 "$TMP_EN" "$TMP_VI")

if [ -n "$MISSING_IN_VI" ] || [ -n "$MISSING_IN_EN" ]; then
    echo "❌ strings.xml lệch key giữa values/ và values-vi/:"
    if [ -n "$MISSING_IN_VI" ]; then
        echo "   Thiếu trong values-vi/ (chưa dịch tiếng Việt):"
        echo "$MISSING_IN_VI" | sed 's/^/     /'
    fi
    if [ -n "$MISSING_IN_EN" ]; then
        echo "   Thiếu trong values/ (key thừa ở bản Việt):"
        echo "$MISSING_IN_EN" | sed 's/^/     /'
    fi
    exit 1
fi

echo "✓ strings parity OK ($(wc -l < "$TMP_EN" | tr -d ' ') keys)"
exit 0
