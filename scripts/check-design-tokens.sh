#!/bin/sh
# Gate: cấm hardcode design token ngoài ui/theme/ (refactor-ui-ux.md §7B).
# CHỈ scan staged .kt files trong app/src/main (code Kanso cũ chưa refactor
# không bị chặn — file nào được sửa tới mới phải sạch).
# Pattern bị cấm: Color(0x..., fontSize =, FontFamily.
# Exit 0 = sạch; exit 1 = có vi phạm (in danh sách).

STAGED_KT=$(git diff --cached --name-only --diff-filter=ACM \
    | grep '\.kt$' \
    | grep '^app/src/main/' \
    | grep -v 'ui/theme/')

if [ -z "$STAGED_KT" ]; then
    exit 0
fi

VIOLATIONS=""
for f in $STAGED_KT; do
    # Đọc nội dung STAGED (index), không phải working tree — tránh false pass/fail
    # khi file đã sửa tiếp sau git add.
    CONTENT=$(git show ":$f" 2>/dev/null) || continue
    HITS=$(printf '%s\n' "$CONTENT" | grep -n -E 'Color\(0x|fontSize\s*=|FontFamily\.' | grep -v '^\s*//')
    if [ -n "$HITS" ]; then
        VIOLATIONS="$VIOLATIONS$f:
$(printf '%s\n' "$HITS" | sed 's/^/    /')
"
    fi
done

if [ -n "$VIOLATIONS" ]; then
    echo "❌ Hardcode design token ngoài ui/theme/ (dùng MaterialTheme.colorScheme/typography hoặc token Dimens):"
    printf '%s' "$VIOLATIONS" | sed 's/^/  /'
    exit 1
fi

echo "✓ design tokens OK"
exit 0
