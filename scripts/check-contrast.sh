#!/bin/sh
# Tính WCAG contrast ratio cho các cặp màu (refactor-ui-ux.md §4B Phase 1 + Phase 9).
# Input: stdin, mỗi dòng "FGHEX BGHEX nhãn..." (hex 6 ký tự, không #).
# Output: ratio + PASS/FAIL theo ngưỡng AA (mặc định 4.5; cột 4 = ngưỡng riêng, vd 3.0 cho large text).
# Exit 1 nếu có cặp FAIL.

awk '
function chan(hex,    v) {
    v = (index("0123456789abcdef", substr(hex,1,1)) - 1) * 16 \
      + (index("0123456789abcdef", substr(hex,2,1)) - 1)
    v = v / 255
    return (v <= 0.03928) ? v / 12.92 : ((v + 0.055) / 1.055) ^ 2.4
}
function lum(hex) {
    hex = tolower(hex)
    return 0.2126*chan(substr(hex,1,2)) + 0.7152*chan(substr(hex,3,2)) + 0.0722*chan(substr(hex,5,2))
}
{
    fg = $1; bg = $2; thr = ($4+0 > 0) ? $4+0 : 4.5
    label = $3
    l1 = lum(fg); l2 = lum(bg)
    hi = (l1 > l2) ? l1 : l2; lo = (l1 > l2) ? l2 : l1
    ratio = (hi + 0.05) / (lo + 0.05)
    status = (ratio >= thr) ? "PASS" : "FAIL"
    if (status == "FAIL") failed = 1
    printf "%-28s #%s / #%s  %.2f:1  (nguong %.1f)  %s\n", label, fg, bg, ratio, thr, status
}
END { exit failed ? 1 : 0 }
'
