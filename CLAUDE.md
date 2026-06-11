# MyBooksLibrary — Project Context

## Stack
Android (Kotlin), Jetpack Compose, Hilt, Room, Coroutines/Flow, Retrofit.
Min SDK 24, Target SDK 35, JDK 21.

## Key Commands
```bash
# Build
JAVA_HOME="C:/Program Files/Java/jdk-21.0.10" ./gradlew assembleDebug

# Unit tests + coverage
./gradlew :app:testDebugUnitTest --console=plain
./gradlew :app:testDebugUnitTest :app:jacocoTestReport

# Lint + ktlint + detekt (chạy trước khi commit)
./gradlew lintDebug ktlintCheck detekt

# Auto-fix formatting
./gradlew :app:ktlintFormat

# Instrumented tests (emulator required)
./gradlew connectedDebugAndroidTest
```

## Coverage Thresholds
- Overall + diff: **85%** (JaCoCo). Ceiling ~90% INSTRUCTION do Compose bytecode.
- `MainNavGraph.kt` (62.77%) và `MainScreens.kt` (48.84%): Compose ceiling, không cố fix.

## Database (Room)
- **Version hiện tại: 4** (`CURRENT_DATABASE_VERSION` trong `AppDatabase.kt`). Schema exported tại `app/schemas/...4.json`.
- Schema v1, v2 không còn — chỉ test migration từ v3 trở đi.
- Khi bump version: cập nhật `PREVIOUS_DATABASE_VERSION`/`CURRENT_DATABASE_VERSION`, viết `Migration(old, new)`, register qua `.addMigrations(...)` trong `AppDatabase.getInstance()`, build để KSP export schema JSON rồi commit, cập nhật `AppDatabaseMigrationTest.kt`.
- Gate chặn bump thiếu migration: `scripts/check-room-migration.sh` — chạy ở cả pre-commit hook (local) lẫn CI job `static-analysis` (check Migration + register + schema JSON).

## Test Patterns
- Compose tests: dùng `createAndroidComposeRule<ComponentActivity>()` (KHÔNG dùng `junit4.v2.createComposeRule` — không generate coverage ổn định; KHÔNG dùng `createEmptyComposeRule` với HiltAndroidTest).
- HiltAndroidTest: `@UninstallModules` để swap fake repository.
- Mỗi test file Compose cần `@HiltAndroidTest` + `@RunWith(AndroidJUnit4::class)`.

## Screenshot Tests (Roborazzi)
- Golden images tại `app/src/test/screenshots/` — record/verify **CHỈ trên CI Linux**, KHÔNG record local (Windows render font khác → ảnh lệch). CI là source of truth.
- Update goldens khi UI đổi có chủ đích: gắn label `record-screenshots` vào PR (gỡ rồi gắn lại để chạy lần nữa), hoặc Actions → "Roborazzi record" → Run workflow sau khi merge.
- `captureRoboImage` là no-op khi chạy `testDebugUnitTest` thường — pre-commit hook không bị ảnh hưởng.
- `ModalBottomSheet`/dialog render window riêng → dùng `captureScreenRoboImage()` thay vì `onRoot().captureRoboImage()`.
- **UI mới → viết `@Preview` (private OK) với fake data** → screenshot test TỰ SINH qua `generateComposePreviewRobolectricTests`, không cần viết `*ScreenshotTest.kt` tay (chỉ viết tay khi cần interaction/state đặc biệt).

## Convention: @Composable không được ở ViewModel/Repository
**ViewModel và data layer không được import `androidx.compose.**`** — `@Composable` function không chạy được trên JVM unit test, kéo coverage xuống.

- `@Composable` helper (vd `UiText.asString()`) → đặt trong `ui/util/` hoặc `ui/screens/`
- Detekt rule `ForbiddenImport` đang enforce: bất kỳ file nào ngoài `ui/screens/`, `ui/navigation/`, `ui/theme/`, `ui/util/` mà import `androidx.compose.**` sẽ bị block.

## Detekt — UndocumentedPublicFunction
Rule đang **bật**. KDoc bắt buộc cho public function, **ngoại trừ** các path sau (tên đã tự document):
- `ui/screens/**`, `ui/navigation/**`, `ui/theme/**`, `ui/viewmodel/**`, `ui/util/**` — Composable/ViewModel
- `data/local/dao/**` — Room DAO (SQL annotations tự document)
- `di/**` — Hilt modules
- `data/local/**Entity.kt`, `data/local/AppDatabase.kt`, `data/local/UserPreferencesDataStore.kt`
- `data/remote/MangaDexApi.kt`, `data/remote/models/**`, `data/remote/NetworkModule.kt`
- `domain/model/**`, `repository/OfflineDownloadRepository.kt`, `repository/GoogleSignInClient.kt`

**Nơi bắt buộc có KDoc**: `data/repository/**` (trừ các file trên) và các public API khác không trong danh sách.

## Files Cần Chú Ý
- `app/src/main/java/.../util/AuthSecrets.kt` — **gitignored** (chứa secret thật). Pre-commit hook tự tạo stub khi cần build.
- `app/schemas/` — commit kèm khi thay đổi Room schema.
- `.gitattributes` — `*.kt eol=lf` đã set, ktlintFormat hoạt động trên Windows.

## Git Workflow
- Branch từ main, PR vào main. Không push thẳng lên main (pre-push hook block).
- Squash merge: `gh pr merge <N> --squash --delete-branch --body ""` (bắt buộc `--body ""` để không thêm Co-authored-by).
- Không dùng `git rebase` khi branch có squash-merged commits từ main → dùng `git merge origin/main`.

## Repo Settings
- Owner: **MinhThang1009** (transfer từ phan-tho, 2026-06-11). URL cũ `phan-tho/MyBooksLibrary` được GitHub redirect.
- Branch protection main: require PR (0 approvals) + required checks `wrapper-validation`, `build-test`, `static-analysis`, `emulator-test`. Không force push/delete.
- Đã bật: auto-merge, Dependency graph, suggest-update-branch, auto-delete head branches.
- Secrets đã set: `GRADLE_ENCRYPTION_KEY` (config-cache CI) + 4 secret ký release (`RELEASE_KEYSTORE_BASE64/PASSWORD`, `RELEASE_KEY_ALIAS/PASSWORD`). Keystore gốc backup ngoài repo (máy owner) — MẤT LÀ KHÔNG UPDATE APP ĐƯỢC.
- Release: push tag `v*` → workflow `release.yml` build AAB/APK ký + GitHub Release + mapping.txt + changelog. Dry-run: chạy workflow_dispatch (build + verify chữ ký, không tạo Release). Secret optional `AUTH_WEB_CLIENT_ID` chưa set → release dùng stub, Google sign-in không hoạt động.
