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
- **Version hiện tại: 3**. Schema exported tại `app/schemas/...3.json`.
- Schema v1, v2 không còn — chỉ test migration từ v3 trở đi.
- Khi bump version: viết `Migration(old, new)` trước, register trong `AppDatabase.getInstance()`, uncomment template trong `AppDatabaseMigrationTest.kt`.
- Pre-commit hook sẽ block nếu bump version mà không có Migration.

## Test Patterns
- Compose tests: dùng `createAndroidComposeRule<ComponentActivity>()` (KHÔNG dùng `junit4.v2.createComposeRule` — không generate coverage ổn định; KHÔNG dùng `createEmptyComposeRule` với HiltAndroidTest).
- HiltAndroidTest: `@UninstallModules` để swap fake repository.
- Mỗi test file Compose cần `@HiltAndroidTest` + `@RunWith(AndroidJUnit4::class)`.

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

## Người Cần Làm (phan-tho)
- Branch protection required checks (Issue #53)
- Enable Dependency graph trong GitHub Settings
- Enable "Always suggest updating PR branches"
