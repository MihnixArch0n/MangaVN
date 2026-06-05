# Kế hoạch hạ tầng test + CI — MyBooksLibrary — 2026-06-04

## Giả định (5 quyết định — dùng default recommend; sửa nếu khác)
1. **UI test = A**: Robolectric + Compose UI test chạy JVM (KHÔNG emulator).
2. **Lint/detekt baseline = CÓ**: tạo baseline cho code cũ để không block CI, fix dần.
3. **AuthSecrets CI = stub giả** (workflow tự ghi file giá trị test).
4. **JaCoCo coverage = CÓ**.
5. **Mutation PIT = CÓ**: module `data.repository` + `data.download`, threshold khởi điểm 60%, chạy nightly.

## ⚠️ Rủi ro số 1 — tương thích tooling (✅ XÁC NHẬN bằng build ở Đợt 1 — 2026-06-04)
Stack: AGP **9.2.1** + Kotlin **2.3.21** + Gradle **9.4.1** (rất mới). **Ground truth từ build Đợt 1** (đè giả định verify-plan post-cutoff — version `2.0.0-alpha.3` HƯ CẤU, không tồn tại):
- `detekt = "1.23.8"` ✅ — version mới nhất thực tế (KHÔNG có bản 2.x trên Maven Central / Gradle Plugin Portal). **Parse được Kotlin 2.3.21** (detektBaseline BUILD SUCCESSFUL, 340 finding code cũ vào baseline). Chạy OK trên Gradle 9.4.1 (chỉ deprecation warning cho Gradle 10 tương lai). → Lo ngại "1.23.x không đọc được Kotlin 2.3.x" KHÔNG xảy ra với chế độ syntax-only (không type resolution).
- `ktlint-gradle = "14.2.0"` ✅ — tồn tại, parse Kotlin 2.3.21 OK (ktlintGenerateBaseline BUILD SUCCESSFUL). Baseline tại `app/config/ktlint/baseline.xml`.
- `pitest-gradle = "1.19.0"` — test tới Gradle 9.0; **chưa xác nhận trên 9.4.1** → thử thực tế, fallback nếu lỗi.
- `mockwebserver` (OkHttp 5) → dùng artifact mới **`com.squareup.okhttp3:mockwebserver3`** (package `mockwebserver3.*`), KHÔNG dùng legacy `mockwebserver`.
- Hilt test: dùng **`kspTest`** (project Hilt qua KSP, không kapt); `hilt-android-testing` = `2.59.2` (khớp `hilt` catalog).
- `work-testing:2.11.2` / `room-testing:2.8.4` — AndroidX cùng version với runtime; xác nhận bằng `./gradlew dependencies` lúc implement.

**Nguyên tắc**: nếu plugin nào build fail vì incompat → hoãn riêng plugin đó, KHÔNG block cả đợt (vd detekt alpha lỗi → tạm bỏ detekt, giữ ktlint + lint).

## What does NOT change
- Logic production app (chỉ thêm test/config/workflow).
- Test "chứng minh bug" audit (gitignored) — giữ local, KHÔNG đưa vào CI/coverage. Đợt 2 sẽ viết **bản tracked chính thức** thay thế (assertion = hành vi đúng sau fix).
- PR workflow team.

## Pre-flight (Phase 0 — làm đầu MỖI đợt)
- `$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'; .\gradlew.bat :app:testDebugUnitTest --console=plain` → ghi **BASELINE** = số test tracked pass. (Lưu ý: audit test gitignored ở filesystem local có thể làm sai số đếm — khi đo baseline cho CI, chỉ tính test tracked.)
- `git status` sạch; tạo branch mới mỗi đợt.

---

## ĐỢT 1 — Static + CI  [1 PR; ~0.5–1 ngày; rủi ro thấp]

### 1.1 detekt + ktlint
- `gradle/libs.versions.toml`: thêm version + plugin `io.gitlab.arturbosch.detekt`, `org.jlleitschuh.gradle.ktlint`. **(version: verify compat AGP 9.2.1)**
- Apply ở `build.gradle.kts` (root) hoặc `app/build.gradle.kts`.
- `config/detekt/detekt.yml` (từ `detektGenerateConfig`).
- Baseline code cũ: `./gradlew detektBaseline` → `config/detekt/baseline.xml`; `./gradlew ktlintFormat` 1 lần (hoặc baseline).
- **Verify**: `./gradlew detekt ktlintCheck lintDebug` → pass (sau baseline).
- **Test gate**: `:app:testDebugUnitTest` = BASELINE (không đụng test).

### 1.2 CI GitHub Actions (unit + static)
- `.github/workflows/ci.yml`: `on: [push (main), pull_request]`; job `build-test` (ubuntu-latest):
  - `actions/checkout` → `actions/setup-java@v4` (temurin 21) → `gradle/actions/setup-gradle` (cache) →
  - **step "stub AuthSecrets"**: ghi `app/src/main/java/com/example/mybookslibrary/util/AuthSecrets.kt` với `object AuthSecrets { const val WEB_CLIENT_ID = "test-web-client-id.apps.googleusercontent.com" }` →
  - `./gradlew :app:testDebugUnitTest lintDebug detekt ktlintCheck`
  - (optional) `actions/upload-artifact` test report.
- **Verify**: push branch → tab Actions xanh; PR hiện check `build-test`.

---

## ĐỢT 2 — Unit/Robolectric lấp gap  [1 PR; ~1–2 ngày; phần NẶNG nhất]

Viết test **tracked** cho class chưa phủ. Mỗi mục = file test + ca kiểm chính:

- **AuthRepository** (mock UserDao/prefs + `UnconfinedTestDispatcher`): register (2 user cùng pass → hash khác = salt); login đúng/sai; re-hash legacy SHA-256 khi login; verifyPassword hash hỏng → false. *(bản tracked của `AuthRepositoryPasswordHashTest`)*
- **MangaRepository** (mock api): getChapterDelivery ok→delivery, error-envelope→failure; `ChapterDelivery.pageUrl` out-of-range→`IllegalArgumentException`; mapping DTO→domain; `getMangaFeed` pagination break.
- **LibraryRepository** (Room in-memory Robolectric): `restoreItems` @Upsert KHÔNG cascade-xóa progress; `updateReadingProgress`/`markChapterCompleted`/`markChapterUnread` GIỮ `is_downloaded`. *(bản tracked của `LibraryRepositoryRestoreTest` + `LibraryRepositoryDownloadFlagTest`)*
- **OfflineDownloadRepository** (Room in-memory): markChapterDownloaded; markChapterNotDownloaded → `clearDownloadedChapterFlag`.
- **OfflineDownloadStorage** (temp dir): savePage; getChapterPages; markChapterComplete ném khi no-page; scanDownloadedChapters (cần marker + page); backfillCompletionMarkers.
- **UserPreferencesDataStore** (fake DataStore ném IOException): read → `.catch` → default. *(bản tracked của `UserPreferencesDataStoreErrorTest`)*
- **Room DAO** (in-memory): LibraryDao @Upsert; ChapterDao `upsertReadingProgress` preserve flag; DownloadQueueDao.
- **AppDatabase**: cấu hình không destructive (giữ dữ liệu / fail-loud). *(bản tracked của `AppDatabaseDestructiveMigrationTest`)*

### 2.1 JaCoCo
- Thêm jacoco plugin + `jacocoTestReport` gắn `testDebugUnitTest`. **Verify**: `./gradlew jacocoTestReport` → html report; xem coverage `data.repository`/`data.download`.
- **Test gate**: `testDebugUnitTest` tăng số test, pass.

---

## ĐỢT 3 — Integration + UI + Mutation  [1 PR hoặc tách; ~1–2 ngày; rủi ro cao]

### 3.1 Integration (work-testing + room-testing)
- Deps testImpl: `androidx.work:work-testing` (2.11.2), `androidx.room:room-testing` (2.8.4), `com.squareup.okhttp3:mockwebserver3` (5.3.2 — artifact OkHttp 5, không dùng legacy `mockwebserver`).
- **ChapterDownloadWorker** (Robolectric + `TestListenableWorkerBuilder` + custom WorkerFactory): mock `getChapterDelivery` (3 trang), `MockWebServer` cho image client, mock storage/repo. Ca: tải xong→`markChapterComplete` + success; markComplete ném (no page)→handled; cancel→re-throw không ghi ERROR. *(Lưu ý: `setForeground` trong test cần `WorkManagerTestInitHelper` — xử lý hoặc tách logic.)*
- **room-testing**: `MigrationTestHelper(schemaLocation=app/schemas)` → `createDatabase(v3)` valid (khung cho migration version sau).

### 3.2 UI critical (Robolectric + Compose — cơ chế A)
- Deps: chuyển `androidx-compose-ui-test-junit4` + `ui-test-manifest` sang **testImplementation**; thêm `hilt-android-testing` (testImpl) + `hilt-android-compiler` (kspTest). `@Config` HiltTestApplication / custom runner; `GraphicsMode.NATIVE`.
- Ca: **navigation** `signOut→Login` (loggedInUserId→null đẩy về Login); **reader** render trang + tap zone.
- **Verify**: `testDebugUnitTest` (Robolectric chạy compose) pass.
- **RISK**: Hilt+Compose+Robolectric phức tạp → **làm spike nhỏ trước**; nếu không khả thi → fallback cơ chế B (androidTest + emulator job + `.github/workflows/ui.yml`).

### 3.3 Mutation testing (PIT)
- Plugin `info.solidsoft.gradle.pitest`. config: `targetClasses = ["...data.repository.*","...data.download.*"]`, `mutationThreshold = 60`, threads. Kotlin: `excludedMethods`/`avoidCallsTo` để giảm mutant synthetic (cân nhắc `arcmutate-kotlin`).
- **Verify**: `./gradlew pitest` → report + score ≥ 60% module critical.
- CI: `.github/workflows/mutation.yml` (`on: schedule cron nightly`) — KHÔNG chặn PR.

---

## Quality gate / threshold (ngưỡng phải đạt trước merge/push)

### Gate CỨNG — chặn merge PR (qua CI + branch protection)
- **Test pass 100%**: `:app:testDebugUnitTest` xanh.
- **Static clean**: `detekt` + `ktlintCheck` + `lintDebug` không vi phạm mới (dưới baseline).
- Gom vào task `./gradlew check` (test + lint + detekt + ktlint + coverage verification) → CI chạy `check`.

### Branch protection (GitHub — setting repo, MANUAL step)
- Settings → Branches → rule cho `main`: ✅ Require status checks to pass (chọn check `build-test`) + ✅ Require PR before merging + (optional) Require branches up to date.
- Hoặc CLI (cần quyền admin repo): `gh api -X PUT repos/phan-tho/MyBooksLibrary/branches/main/protection ...`.
- Sau khi bật: PR đỏ CI → nút Merge bị khóa. *(Đây là gate THẬT, không bypass được.)*

### Coverage gate (JaCoCo — RATCHET, lỏng → siết)
- `jacocoTestCoverageVerification`: `minimum = 0.60` khởi điểm (có thể cao hơn cho `data.repository`/`data.download`), gắn vào `check`.
- ⚠️ KHÔNG đặt cao ngay (≥80%) → tránh "game test" (viết test giả lấy số). Siết dần, hoặc dùng quy tắc "không cho tụt".

### Mutation gate (PIT — KHÔNG chặn PR)
- `mutationThreshold = 60` chỉ ở job **nightly** (`mutation.yml`), không gate merge (quá chậm). Theo dõi xu hướng, siết dần.

### Local git hooks (OPTION — không bắt buộc; chỉ là lớp tiện lợi, dễ bị `--no-verify`)
- **pre-commit** (nhanh, vài giây): `./gradlew ktlintCheck detekt` — bắt lỗi style sớm trước khi commit.
- **pre-push** (chậm hơn): `./gradlew testDebugUnitTest` — tránh push code đỏ lên CI.
- `.git/hooks/` KHÔNG commit được → quản lý qua repo bằng `scripts/git-hooks/` + task Gradle copy vào `.git/hooks` (hoặc plugin `com.star-zero.gradle.githook` / Husky-for-Android). Cài 1 lần mỗi máy.
- ⚠️ Hook local KHÔNG thay gate CI — chỉ giảm vòng lặp đỏ. Gate THẬT vẫn là CI + branch protection.

### Nguyên tắc độ cứng (tránh phản tác dụng)
- **Cứng** (chặn merge): test pass + static clean.
- **Ratchet** (cảnh báo, siết dần): coverage.
- **Theo dõi** (không chặn): mutation.

### Implement ở đợt nào
- **Đợt 1**: task `check` aggregate + bật **branch protection** (sau khi CI có check `build-test`) + (option) local hooks.
- **Đợt 2**: `jacocoTestCoverageVerification` (cùng JaCoCo).
- **Đợt 3**: mutation threshold (đã nêu §3.3).

## Files touched — danh sách
- `gradle/libs.versions.toml`, `build.gradle.kts` (root), `app/build.gradle.kts`
- `config/detekt/detekt.yml`, `config/detekt/baseline.xml`
- `.github/workflows/ci.yml`, `.github/workflows/mutation.yml` (+ `ui.yml` nếu cơ chế B)
- (option) `scripts/git-hooks/` + task Gradle copy hook vào `.git/hooks`
- Nhiều file `app/src/test/...` (đợt 2 + 3.1), `app/src/androidTest/...` (chỉ nếu cơ chế B)
- Branch protection `main` — setting trên GitHub (không phải file repo)

## Rollback / an toàn
- Mỗi đợt làm trên **branch riêng → 1 PR**. Chưa merge → rollback = đóng PR / xóa branch (`main` không đổi).
- Mỗi sub-step commit nhỏ riêng → lỗi thì `git revert <commit>` hoặc reset branch về trước.
- Plan **chỉ thêm test/config/workflow, KHÔNG đụng code production** → rollback an toàn tuyệt đối với app (gỡ config/test không ảnh hưởng logic chạy).
- Đã merge mà cần undo → `git revert -m 1 <merge-commit>` (revert nguyên PR).
- Branch protection nếu đã bật mà chặn hotfix gấp → admin tạm tắt rule rồi bật lại; KHÔNG `--no-verify` / force-push `main`.

## Post-implementation checks (mỗi đợt)
- `testDebugUnitTest` ≥ BASELINE, pass; CI xanh trên PR.
- `git diff` KHÔNG đụng file production (chỉ test/config/workflow). Nếu có → dừng, đó là scope creep.

## Context re-read sau /compact
- File này (`.claude/test-plan.md`), memory `no-subagents-preference` + `audit-findings-2026-06`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.claude/handoff.md`.

## Quy trình duyệt (plan-refactor)
- Gate 1 (scope) ✓ — user chốt 5 tầng + mutation.
- verify-plan ✓ → **Gate 2 ✓** (user duyệt 2026-06-04) → **Đợt 1 IMPLEMENT XONG** trên branch `chore/static-analysis-ci` (chưa commit/push, chờ user).
- TIẾP: user duyệt commit/push/PR Đợt 1 → rồi Đợt 2 (unit/Robolectric gap + JaCoCo).

## Trạng thái Đợt 1 (2026-06-04)
- ✅ detekt 1.23.8 + ktlint 14.2.0 apply ở `app/build.gradle.kts`; version ở `gradle/libs.versions.toml`.
- ✅ Baseline code cũ: `config/detekt/baseline.xml` (340 finding) + `app/config/ktlint/baseline.xml`. detekt config: `config/detekt/detekt.yml`.
- ✅ CI: `.github/workflows/ci.yml` (stub AuthSecrets → testDebugUnitTest + lintDebug + detekt + ktlintCheck; YAML valid; `gradle/actions/setup-gradle@v4` xác nhận tồn tại).
- ✅ Verify: `detekt ktlintCheck lintDebug` BUILD SUCCESSFUL (với baseline); `testDebugUnitTest` = BASELINE 87 pass; `check` tự gom detekt+ktlint+test+lint. Diff KHÔNG đụng `app/src/main`.
- ⏳ CÒN (manual, cần user/quyền admin): bật **branch protection** `main` require check `build-test` (sau khi CI chạy lần đầu trên PR để check xuất hiện).
