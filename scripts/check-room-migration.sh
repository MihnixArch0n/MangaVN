#!/bin/sh
# Gate Room migration (issue #94): chặn bump DB version mà thiếu Migration/register/schema.
#
# Usage: check-room-migration.sh <old_ref> <new_ref>
#   old_ref: git ref chứa AppDatabase.kt TRƯỚC thay đổi (vd "HEAD", "origin/main")
#   new_ref: git ref SAU thay đổi — "" (chuỗi rỗng) nghĩa là staged index (dùng cho pre-commit hook)
#
# Hỗ trợ cả 2 form khai báo version:
#   - constant: CURRENT_DATABASE_VERSION = N (+ PREVIOUS_DATABASE_VERSION = M)
#   - literal:  version = N
# Form constant trong Migration(PREVIOUS_..., CURRENT_...) chỉ được chấp nhận khi
# PREVIOUS ở bản mới == CURRENT ở bản cũ (đảm bảo migration cover đúng bước nhảy).

DB_FILE="app/src/main/java/com/example/mybookslibrary/data/local/AppDatabase.kt"
SCHEMA_DIR="app/schemas/com.example.mybookslibrary.data.local.AppDatabase"

OLD_REF="$1"
NEW_REF="$2"

show_content() {
    # git show "<ref>:<file>" — ref rỗng = staged index (":<file>")
    git show "$1:$DB_FILE" 2>/dev/null
}

extract_version() {
    # $1 = nội dung file, $2 = tên constant; fallback literal "version = N"
    ver=$(printf '%s' "$1" | grep -o "$2 = [0-9][0-9]*" | grep -o '[0-9][0-9]*' | head -1)
    if [ -z "$ver" ] && [ "$2" = "CURRENT_DATABASE_VERSION" ]; then
        ver=$(printf '%s' "$1" | grep 'version = [0-9]' | grep -o '[0-9][0-9]*' | head -1)
    fi
    printf '%s' "$ver"
}

OLD_CONTENT=$(show_content "$OLD_REF")
NEW_CONTENT=$(show_content "$NEW_REF")

# File chưa tồn tại ở một trong hai phía (repo mới / file vừa tạo) → không so sánh được, bỏ qua.
if [ -z "$OLD_CONTENT" ] || [ -z "$NEW_CONTENT" ]; then
    exit 0
fi

OLD_VER=$(extract_version "$OLD_CONTENT" "CURRENT_DATABASE_VERSION")
NEW_VER=$(extract_version "$NEW_CONTENT" "CURRENT_DATABASE_VERSION")

if [ -z "$OLD_VER" ] || [ -z "$NEW_VER" ]; then
    echo "⚠️  check-room-migration: không extract được version từ $DB_FILE — kiểm tra lại pattern trong script."
    exit 1
fi

if [ "$OLD_VER" = "$NEW_VER" ]; then
    exit 0
fi

echo ">>> Room DB version bump phát hiện: $OLD_VER → $NEW_VER. Kiểm tra migration..."
FAIL=0

# Check 1: Migration(old, new) tồn tại — literal hoặc constant form.
HAS_LITERAL=$(printf '%s' "$NEW_CONTENT" | grep -c "Migration($OLD_VER, $NEW_VER)")
HAS_CONSTANT=$(printf '%s' "$NEW_CONTENT" | grep -c "Migration(PREVIOUS_DATABASE_VERSION, CURRENT_DATABASE_VERSION)")
if [ "$HAS_CONSTANT" -gt 0 ]; then
    NEW_PREV=$(extract_version "$NEW_CONTENT" "PREVIOUS_DATABASE_VERSION")
    if [ "$NEW_PREV" != "$OLD_VER" ]; then
        echo "❌ Migration(PREVIOUS_..., CURRENT_...) tồn tại nhưng PREVIOUS_DATABASE_VERSION = $NEW_PREV ≠ $OLD_VER (version cũ)."
        echo "   Cập nhật PREVIOUS_DATABASE_VERSION = $OLD_VER và viết logic migrate $OLD_VER → $NEW_VER."
        FAIL=1
    fi
elif [ "$HAS_LITERAL" -eq 0 ]; then
    echo "❌ Không tìm thấy Migration($OLD_VER, $NEW_VER) trong $DB_FILE."
    echo "   Viết migration trước khi bump version — thiếu nó app sẽ crash khi user upgrade"
    echo "   (project đã bỏ fallbackToDestructiveMigration để không xóa data người dùng)."
    FAIL=1
fi

# Check 2: migration được register qua .addMigrations(...) trong getInstance().
if ! printf '%s' "$NEW_CONTENT" | grep -q '\.addMigrations('; then
    echo "❌ Không thấy .addMigrations(...) trong $DB_FILE — migration chưa được register trong AppDatabase.getInstance()."
    FAIL=1
fi

# Check 3: schema JSON của version mới đã export (chạy build với KSP room.schemaLocation rồi commit).
if [ ! -f "$SCHEMA_DIR/$NEW_VER.json" ]; then
    echo "❌ Thiếu schema export: $SCHEMA_DIR/$NEW_VER.json"
    echo "   Chạy ./gradlew :app:assembleDebug để KSP export schema, rồi commit file JSON."
    FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
    echo ""
    echo "Gate Room migration FAILED — xem CLAUDE.md §Database (Room) cho quy trình bump version."
    exit 1
fi

echo ">>> Gate Room migration OK: Migration $OLD_VER→$NEW_VER + register + schema đầy đủ."
exit 0
