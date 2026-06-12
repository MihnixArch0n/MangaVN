# Kế hoạch Refactor UI/UX + Onboarding — MyBooksLibrary

> Tài liệu plan tổng hợp (Bước 1–3 đã được duyệt qua hội thoại 2026-06-11).
> Branch thực hiện: `refactor/ui-ux-redesign` (từ `main`). Commit nhỏ theo từng phase, KHÔNG gộp.
> Trạng thái từng màn hình theo dõi ở §9 (Checklist phạm vi) — cập nhật sau mỗi phase.

---

## 1. Bối cảnh & phát hiện (Bước 1 — đã xác nhận)

- **App**: client đọc manga cho MangaDex API. Đối tượng: người đọc manga, song ngữ EN/VI.
- **Stack**: 100% Jetpack Compose, Material 3 (BOM `2026.05.01` — M3 Expressive đã stable), Kotlin 2.3.21, Navigation Compose type-safe, Hilt, Room (DB schema v4 — `CURRENT_DATABASE_VERSION`, lib 2.8.4), DataStore, Coil 3, Telephoto. `SharedTransitionLayout` đã wire sẵn trong `MainNavGraph.kt`.
- **Theming hiện tại**: design system "Kanso Editorial" (serif + ink/paper/terracotta) — sẽ **thay thế hoàn toàn** bằng hướng mới (quyết định của user).
- **Localization**: `values/strings.xml` + `values-vi/strings.xml`, 159 key parity 100%. App dùng custom `appString()` + `LocalAppLocale` (`ui/util/LocaleHelper.kt`) — **KHÔNG dùng `stringResource()`**; đổi ngôn ngữ runtime không recreate Activity. Mọi UI mới phải theo cơ chế này.
- **Hardcode duy nhất tìm thấy**: `Text("All")` tại `ui/screens/MangaDetailScreen.kt:252` — fix ở Phase 4.
- **Test**: ~104 file unit test (con số xê dịch ±2 tùy cách đếm helper/fixture), coverage gate **85%** (overall + diff), Roborazzi **23 goldens** (record/verify CHỈ trên CI Linux qua label `record-screenshots`), instrumented test cần emulator (required check CI).
- **Onboarding hiện tại**: chưa có gì. `UserPreferencesDataStore` chưa có flag first-launch.
- **Orientation/responsive**: app không khóa orientation (xoay được, state sống nhờ ViewModel/DataStore) nhưng **0 chỗ dùng `WindowSizeClass`** — layout phone-portrait cứng. Yêu cầu bổ sung: responsive + xoay ngang/dọc.
- **Reduce-motion**: chưa xử lý ở đâu (đã verify grep = 0).

## 2. Quyết định đã chốt (user duyệt — KHÔNG mở lại)

| # | Quyết định | Lựa chọn | Phương án bị loại |
|---|---|---|---|
| 1 | Hướng thẩm mỹ | **Cinematic dark-first** — content-forward kiểu Netflix/Crunchyroll, dark là gốc, cover art làm hero, accent rực trên nền tối | M3 Expressive vibrant thuần (thiếu cá tính); Calm minimal (ít "mới") |
| 2 | Pattern onboarding | **Hybrid 2 tầng**: welcome carousel 3 trang trước Login + contextual spotlight lần đầu vào Reader | Carousel-đơn (không dạy được gesture ẩn Reader); Spotlight-đơn (overlay infra rộng, giá trị thấp ở màn tự giải thích) |
| 3 | Font | **Bricolage Grotesque** (display — variable wght/wdth/opsz, tiếng Việt chính thức, cố vấn dấu: Nhung Nguyen) + **Be Vietnam Pro** (body/UI — designer Việt thiết kế). Đã bundle Phase 0: Bricolage **variable 1 file** (minSdk 30 dùng tốt `FontVariation.Settings`) + BVP 4 static weight — tổng ~950KB | Be Vietnam Pro đơn (ít wow); Inter (vô danh) |
| 4 | Tiêu chí "đẹp" | **Wow tức thì với người dùng** (font, UI, hiệu ứng) — ưu tiên ấn tượng thị giác hơn sự dè dặt kỹ thuật, nhưng vẫn giữ hiệu năng + reduce-motion | — |
| 5 | User cũ đã đăng nhập | KHÔNG thấy welcome carousel (set flag im lặng), chỉ thấy reader hint | — |
| 6 | Dependency mới | `material3-window-size-class` (cùng Compose BOM sẵn có — không thêm version mới) + font files + `androidx.core:core-splashscreen` (Phase 2, duyệt tại GATE 1) | `material3.adaptive:adaptive-*` (nặng, không cần — nav rail tự làm từ FloatingPillNavBar) |
| 7 | Glass effect | **Glassmorphism 2.0 tiết chế** — tối đa 3 panel/màn: nav pill, reader bars, header bottom sheet. Backdrop blur thật qua thư viện **Haze** (chốt sau 🔍 research Phase 2 — dependency mới cần user duyệt tại gate; fallback translucent nếu không duyệt). Chất "liquid" làm bằng spring motion + shape morph M3 Expressive sẵn có | Liquid Glass khúc xạ AGSL kiểu Apple (đòi API 33+ trong khi minSdk 30, lib alpha, khúc xạ đè trang truyện khó đọc) |
| 8 | Icon set | **Lucide** (`com.composables:icons-lucide` **1.1.0**, MIT — biến thể ImageVector; ⚠️ KHÔNG nhầm với `icons-lucide-android` 2.2.1 là biến thể XML drawable) — bộ icon phổ biến nhất 2024–2026, stroke mảnh đồng nhất hợp chất cinematic. Migrate dần theo từng phase màn hình; `material-icons-extended` gỡ ở Phase 9 khi hết usage | Material Symbols (chưa có artifact Compose chính thức); Phosphor (đẹp nhưng ít phổ biến hơn) |

## 3. Design system "Cinema" (Phase 1 — làm TRƯỚC mọi màn hình)

### 3.1 Color tokens — dark là gốc, light phái sinh

Lý do: nền gần-đen pha xanh (tránh OLED smearing, giữ được depth), tách lớp bằng 4 bậc luminance thay shadow; accent **vermilion** (ngôn ngữ thị giác manga/anime); tertiary **gold** cho rating.

| Token | Dark (gốc) | Light (phái sinh) | Dùng cho |
|---|---|---|---|
| `background` | `#0E0F13` | `#F7F7F9` | nền màn hình |
| `surface` | `#16181D` | `#FFFFFF` | bar, sheet nền |
| `surfaceContainer` | `#1E2128` | `#FFFFFF` + shadow mềm | card, shelf item |
| `surfaceContainerHigh` | `#262A33` | `#FFFFFF` + shadow đậm | dialog, bottom sheet |
| `primary` | `#FF5A3C` | `#C8361B` (đậm hơn để đạt AA nền sáng) | CTA, indicator, link |
| `onPrimary` | `#1C0D08` | `#FFFFFF` | text trên primary |
| `tertiary` | `#FFC24B` | `#9A6A00` | rating, badge |
| `onSurface` | `#E9EAEE` | `#1A1C22` | text chính |
| `onSurfaceVariant` | `#9BA0AC` | `#5A5E68` | text phụ |
| `outline` | trắng 12% | đen 12% | border cover, divider |

> Hex là thiết kế ban đầu — **bắt buộc verify WCAG AA** (≥4.5:1 body, ≥3:1 large text) bằng tool khi implement, audit tổng ở Phase 9. Status colors (success/warning) giữ pattern cặp light/dark riêng, đổi giá trị theo palette mới.

### 3.2 Typography — hybrid 2 font

- **Bricolage Grotesque**: displayLarge/Medium/Small, headlineLarge/Medium — hero title 40–44sp, weight SemiBold/Bold, tracking âm nhẹ; tên truyện dài tiếng Việt → dùng cut condensed (width axis) thay vì cắt chữ.
- **Be Vietnam Pro**: title/body/label — body lineHeight ≥1.45× (dấu 2 tầng "ễ", "ậ" cần khoảng thở); **bỏ all-caps tracking 1.4sp** ở labelLarge (chuỗi VI dài hơn EN ~35%, dấu + tracking lớn = tràn nút).
- Mọi style preview với **chuỗi VI dài nhất thực tế** trong strings.xml, không chỉ EN.
- File font (đã tải Phase 0): `bricolage_grotesque.ttf` (variable — dùng `FontVariation.Settings(wght, wdth)`; **minSdk thực tế = 30**, không phải 24 như CLAUDE.md ghi — CLAUDE.md stale, cần sửa riêng) + `be_vietnam_pro_{regular,medium,semibold,bold}.ttf`. License OFL tại `assets/licenses/`.

### 3.3 Spacing / Shape / Elevation / Motion tokens

- `ui/theme/Dimens.kt` (mới): grid 4dp — `4/8/12/16/24/32`; screen padding 16 (compact) / 24 (medium+).
- `ui/theme/Shape.kt` (mới): small 8, medium 12, large 16, extraLarge 24, cover art 10, pill = full.
- Elevation: dark = surface container 4 bậc + **tắt `surfaceTint`** (set = surface, tránh ám tím M3); light = shadow mềm 2 lớp.
- `ui/theme/Motion.kt` (mới, tạo ở Phase 1): duration tokens `Fast 150ms / Default 250ms / Emphasized 400ms` (easing: dùng nguyên `MotionScheme.expressive()` của M3 qua MaterialTheme, không tự chế curve) + `LocalReducedMotion` (định nghĩa tại đây, đọc `Settings.Global.ANIMATOR_DURATION_SCALE == 0`).
- Elevation light mode (giá trị chốt, khỏi đoán): card nghỉ `2dp`, card nhấn/kéo `6dp`, dialog/sheet `8dp` — shadow mềm; dark mode KHÔNG dùng shadow, chỉ 4 bậc surface container như trên.

## 3B. Blueprint bố cục & navigation chrome (bổ sung theo yêu cầu user 2026-06-11)

> Đây là spec bố cục từng màn — các phase ở §4 implement THEO blueprint này. Mỗi màn có 2 biến thể: portrait (compact width) và landscape/medium+ (đọc từ `WindowSizeClass`).

### Navigation chrome (thanh menu)
- **Portrait**: floating pill bar đáy màn — 4 tab (Discover/Search/Library/Settings), glass blur (panel #1), indicator morph spring chạy theo tab, label chỉ hiện ở tab active (icon + text), tự ẩn khi cuộn xuống / hiện khi cuộn lên (behavior mới).
- **Landscape / medium+**: pill chuyển thành **rail dọc trái** (cùng component, layout dọc), content full chiều ngang còn lại.
- Màn không có nav (Login/Register/Reader/Detail/Review/Onboarding): giữ nguyên danh sách loại trừ hiện có trong `MainNavGraph.kt`.

### Blueprint từng màn

| Màn | Portrait | Landscape/medium+ |
|---|---|---|
| **Discover** | Hero spotlight full-bleed ~50% chiều cao (pager ngang, cover bleed + scrim, title Bricolage 40sp+, CTA), dưới là shelves ngang (Popular/New Releases) card nổi, cuối là grid Explore | Hero co còn ~40% dạng banner 2 cột (cover trái, info phải); shelves nhiều item hơn |
| **Search** | Search bar lớn đầu màn (pill, glass khi cuộn), chip filter ngang dưới bar, grid kết quả 3 cột (cover 2:3) | Grid 5–6 cột; search bar + chips gọn 1 hàng |
| **Library** | Chip trạng thái (Đang đọc/Hoàn thành/Yêu thích) dưới title, grid 3 cột, empty state illustration giữa màn | Grid 5–6 cột |
| **Detail** | Hero edge-to-edge ~45% (parallax khi cuộn), title + meta + rating đè scrim, hàng CTA (Đọc ngay primary + Lưu + Share), synopsis collapse, chapter list | Cover cố định 1/3 trái, cột phải cuộn (info + chapters) |
| **Review** | List review card, header theo design system | Content max-width giữa màn |
| **Reader** | Giữ immersive hiện tại; bars glass top/bottom (panel #2), page slider đáy | Giữ nguyên cơ chế (3 chế độ đọc đã có); bars như portrait |
| **Settings** | Header account trên đầu, section card (Giao diện & Ngôn ngữ / Đọc / Lưu trữ / Dữ liệu / Liên kết / Tài khoản / **Hướng dẫn**) | Content max-width ~600dp căn giữa |
| **Login/Register** | Logo/brand trên, form căn giữa 1 cột | 2 cột: brand trái, form phải |
| **Onboarding carousel** | Illustration ~60% trên, text + dots + nút dưới | Illustration trái, text phải |

Quy tắc chung: screen padding theo Dimens (16/24), cover ratio 2:3 thống nhất toàn app, KHÔNG đổi navigation flow giữa các màn (chỉ đổi bố cục bên trong màn — ngoại lệ duy nhất: destination Onboarding đã duyệt).

## 3C. Premium micro-details (sweep lần 2, 2026-06-11 — gap tự phát hiện sau câu hỏi phản biện của user)

| # | Gap | Phase xử lý | Spec |
|---|---|---|---|
| 1 | Predictive back gesture (Android 14+) | 2 | `android:enableOnBackInvokedCallback="true"` trong manifest + verify back-preview animation với NavHost/shared element |
| 2 | TalkBack cho spotlight onboarding | 8 | Overlay có semantics mô tả nội dung hint; nút "Bỏ qua" focusable; TalkBack đọc được trình tự hướng dẫn |
| 3 | Status bar icon tàng hình trên hero sáng | 3, 4 | Scrim gradient đỉnh màn (đen→trong) cố định trên hero Detail/Discover |
| 4 | Ô trắng khi cover đang load | 1B | `MangaCoverCard`: placeholder `surfaceContainer` + crossfade 200ms thống nhất (Coil) — không bao giờ trắng trơ |
| 5 | Bàn phím che input | 7 | `imePadding()` cho form Login/Register + Search; verify bằng preview/test |
| 6 | Notch ăn vào trang truyện (Reader landscape) | 5 | `layoutInDisplayCutoutMode` phù hợp cho reader immersive; verify trên emulator có cutout |
| 7 | fontScale 2.0 vỡ hero | 9 | Audit fontScale 2.0: hero title maxLines + ellipsis, nút không tràn; chuẩn preview vẫn 1.3, 2.0 check ở audit |

Backlog hỏi user (đụng behavior, KHÔNG tự làm): pull-to-refresh cho Discover/Library (gọi reload ViewModel = đụng logic ngoài scope đã duyệt).

**Goldens timeline**: 23 goldens hiện tại = baseline Kanso (đã commit trên main). Mỗi phase đổi UI → re-record qua label `record-screenshots` trên PR của phase đó (diff lớn là KỲ VỌNG khi đổi design — verify bằng mắt trên CI artifact thay vì tin diff %). Sau GATE 5 (Phase 9): goldens LOCK — từ đó mọi diff là regression thật.

## 4. Các phase tuần tự

> Mỗi phase = ≥1 commit hoàn chỉnh (Conventional Commits, subject tiếng Việt). Xác minh trước MỖI commit: xem §8.
> 🛑 = điểm dừng báo cáo tiến độ + user kiểm tra bằng mắt trên thiết bị, CẢ HAI ngôn ngữ, trước khi đi tiếp.

### Phase 0 — Hạ tầng (S)
- Tạo branch `refactor/ui-ux-redesign` từ `main`.
- **Baseline test** (P4): **626 test pass / 0 fail** (đã chạy 2026-06-11, BUILD SUCCESSFUL 3m06s) — mọi phase sau phải giữ ≥ mức này.
- `scripts/check-strings-parity.sh`: fail nếu key `values/strings.xml` ≠ `values-vi/strings.xml`; gắn vào pre-commit hook.
- `scripts/check-design-tokens.sh` (dời từ Phase 1B về đây theo audit): chặn `Color(0x`/`fontSize =`/`FontFamily.` ngoài `ui/theme/` — **chỉ scan staged files** để code Kanso cũ không chặn commit trước khi được refactor tới; gắn vào pre-commit hook.
- Thêm `material3-window-size-class` (BOM-managed) + `com.composables:icons-lucide` **1.1.0** (biến thể ImageVector — xem §2 row 8) vào `libs.versions.toml` + `app/build.gradle.kts`.
- Font files vào `app/src/main/res/font/` (Bricolage Grotesque variable + Be Vietnam Pro 4 weight, license OFL → `assets/licenses/`).

### Phase 1 — Design system (M)
> Gate 🛑 đầu tiên DỜI xuống sau Phase 3 (vertical slice — xem §4B): bảng màu suông không đánh giá được bằng mắt, Discover hoàn chỉnh mới là thước đo "wow" thật.
- Files: `ui/theme/Color.kt` (viết lại), `Theme.kt`, `Type.kt`, `Shape.kt` (mới), `Dimens.kt` (mới), `Motion.kt` (mới).
- Toàn bộ §3. Cập nhật `ThemeTest.kt`. Re-record 23 goldens (label `record-screenshots` trên PR).
- API: `lightColorScheme`/`darkColorScheme`, `MotionScheme.expressive()`, `FontFamily(Font(R.font...))`.

### Phase 1B — Shared component library (M) — đồng nhất bằng cấu trúc
> Yêu cầu user (2026-06-11): "đồng nhất tuyệt đối, không inconsistency, không vỡ layout, không khoảng trắng thừa, không màu lộn xộn". Cơ chế: mọi màn hình CHỈ được ghép từ bộ component này — một chỗ sửa, cả app đổi theo; không màn nào tự vẽ riêng.
- Mới `ui/screens/components/` (hoặc `ui/util/` cho non-visual): `MangaCoverCard` (ratio 2:3 DUY NHẤT toàn app, shape + border outline thống nhất), `SectionHeader` (title + "Xem tất cả"), `StatusChip`, `EmptyState` (illustration + title + subtitle), `ErrorState` (+ nút thử lại), `SkeletonShimmer` (loading placeholder thay spinner trơ — shimmer tắt khi reduce-motion), `AppButton` (primary/secondary/text), `RatingBadge`.
- Gate tự động chống lệch token: `scripts/check-design-tokens.sh` — ĐÃ TẠO ở Phase 0 (xem trên), Phase 1B chỉ cần tuân thủ.
- **Component bổ sung theo audit 2026-06-11**: `StyledDropdownMenu` (container `surfaceContainerHigh`, shape medium — thay 2 chỗ: `ChapterComponents.kt:149`, `DiscoverChromeComponents.kt:98`), `StyledBadge` (primary bg, pill, 18dp, labelSmall — thay Badge ở `SearchScreen.kt:107`), `AppFilterChip` (wrapper FilterChip — selected = primaryContainer bg + onPrimaryContainer text; unselected = surface + onSurfaceVariant; shape medium 12dp; outline 1dp; dùng cho SearchFilterSheet + LanguageFilterRow), `ErrorMessageBox` (container + icon + text — dùng cho lỗi auth thay Text trơ), `LoadingIndicator` wrapper (size token trong Dimens: S `24dp` / M `36dp` / L `48dp`). `DetailMessage` (local ở MangaDetailScreen) hợp nhất vào `EmptyState`/`ErrorState`.
- **Chuẩn preview chống vỡ layout** (áp dụng từ đây cho MỌI screen/component về sau): mỗi screen có preview ở light + dark + **chuỗi VI dài nhất** + **fontScale 1.3** + **width 320dp** + landscape. Tràn chữ/vỡ layout hiện ra ngay ở preview/screenshot test, không đợi lên máy.
- Haptic feedback helper (`LocalHapticFeedback`): chuẩn hóa — confirm nhẹ khi bookmark/chọn tab/swipe carousel; KHÔNG haptic khi cuộn.
- **Icon: Lucide** (`Lucide.X` ImageVector) — từ Phase 1B trở đi mọi icon mới dùng Lucide; icon Material cũ thay dần trong phase của từng màn; Phase 9 gỡ `material-icons-extended` khi grep hết usage.

### Phase 2 — Navigation chrome + splash (M)
- 🔍 **Research trước khi code**: thư viện Haze (Chris Banes) — API hiện tại, chi phí hiệu năng trên LazyList, cơ chế fallback máy yếu/API thấp; so sánh với translucent solid. Kết quả ghi vào plan, trình user duyệt tại **GATE 1 (sau Phase 3)** — Phase 5 mới cần Haze nên quyết định này không chặn Phase 2–4.
- `FloatingPillNavBar.kt`: translucent + **glass blur** (panel kính #1; fallback solid translucent cho máy yếu), indicator morph spring.
- Landscape/medium+ → **rail dọc bên trái** (biến thể của chính FloatingPillNavBar — không thêm dependency); `MainNavGraph.kt` đọc `WindowSizeClass` (provide qua CompositionLocal từ `MainActivity.kt`).
- `AuthLoadingScreen` (MainActivity.kt) → splash có logo/brand.
- **App icon + system splash**: icon mới theo palette Cinema (adaptive icon + themed icon Android 13+) + `androidx.core:core-splashscreen` (Android 12 Splash Screen API) — ấn tượng premium bắt đầu TRƯỚC khi UI hiện. (core-splashscreen là artifact androidx nhỏ, BOM-độc-lập — cần duyệt cùng gate.)
- API: `WindowSizeClass`, `animateDpAsState`/spring, blur (theo kết quả research Haze).

### Phase 3 — Discover (M–L)
- 🔍 **Research trước khi code** (dùng chung cho Phase 3–4): teardown layout thật của app media đầu ngành (Crunchyroll, Webtoon, Netflix mobile) — vị trí CTA trên hero, độ đậm scrim, mật độ chapter list, tỷ lệ cover. Trend cho hướng; teardown cho tỷ lệ cụ thể.
- `DiscoverScreen.kt`, `DiscoverChromeComponents.kt`, `DiscoverShelfComponents.kt`.
- Hero spotlight: cover bleed cạnh + gradient scrim; shelves card nổi (surfaceContainer); **staggered fade-up** khi shelf load lần đầu (mỗi cover trễ ~30ms, respect reduce-motion).
- Landscape: hero 2 cột, shelf nhiều item hơn theo WindowSizeClass.
- API: `Brush.verticalGradient`, SharedTransition (đã wire), `animateItem`.

### Phase 4 — Manga Detail + Review (L) 🛑
- `MangaDetailScreen.kt`, `MangaDetailHeroComponents.kt`, `MangaDetailSections.kt`, `ChapterComponents.kt`, `MangaReviewScreen.kt`, `DetailDimensions.kt`.
- Hero immersive edge-to-edge + **parallax + scale khi cuộn**; scrim đủ đậm để text đạt AA trên mọi cover; chapter list mới.
- **Fix hardcode `Text("All")` tại `ui/screens/MangaDetailScreen.kt:252`** (file màn hình chính — KHÔNG phải wrapper `/detail/`) → key mới `filter_all_languages` ở CẢ EN + VI.
- Spec `ChapterDownloadIndicator` (`ChapterComponents.kt`): size từ Dimens; màu theo trạng thái — downloading = primary, complete = success, error = error; chuyển trạng thái crossfade 200ms.
- Landscape: cover trái + info phải.
- Lưu ý: `ui/screens/detail/MangaDetailScreen.kt` chỉ là wrapper ~20 dòng delegate — không đụng signature.

### Phase 5 — Reader (M)
- `reader/ReaderBars.kt`, `reader/components/PageActionBottomSheet.kt`.
- Restyle theo design system; `LoadingIndicator` (M3 Expressive) thay `CircularProgressIndicator`; reader bars slide+fade tinh chỉnh easing 200ms.
- Reader bars = **panel kính #2** (glass mờ nổi trên trang truyện — vị trí "wow" nhất vì thấy nội dung cuộn phía sau); header `PageActionBottomSheet` = panel kính #3. Chỉ khi Haze đã được duyệt ở GATE 1, ngược lại translucent.
- Toast lỗi save/share (`ReaderEffectHandler.kt:71,187`) → feedback component theo design system (snackbar/toast custom) — **CHỈ đổi cách hiển thị, KHÔNG đổi điều kiện hiển thị/logic**.
- Alpha bars `0.94f` hardcode (`ReaderBars.kt`) → token trong Motion/Dimens.
- KHÔNG đụng logic đọc (tap zones, pager, progress) — chỉ style.

### Phase 6 — Library + Search + FilterSheet (M)
- `LibraryScreen.kt`, `SearchScreen.kt`, `SearchFilterSheet.kt`.
- Grid cover mới (số cột theo WindowSizeClass), empty state illustration vector (màu theo token), filter chips shape morph.

### Phase 7 — Settings + Auth (M) 🛑
- `SettingScreen.kt`: nhóm section card + **thêm mục "Xem lại hướng dẫn"** (điều hướng tới carousel + reset `reader_hint_done`).
- `auth/LoginScreen.kt`, `auth/RegisterScreen.kt`: tối giản trên nền cinematic.

### Phase 8 — Onboarding (L) 🛑 — chi tiết §5
- 🔍 **Research trước khi code**: pattern implement spotlight overlay trong Compose (khoét vùng sáng quanh element + anchor tooltip) — ưu tiên cách tự viết `Canvas` + `BlendMode.Clear`, chỉ cân nhắc lib nếu cách tự viết có vấn đề thực tế.
- Mới: `ui/screens/onboarding/WelcomeCarouselScreen.kt`, `ui/screens/onboarding/ReaderSpotlightOverlay.kt` (+ components).
- `UserPreferencesDataStore`: 2 key mới. `NavigationDestinations.kt`: destination `Onboarding`. `MainNavGraph.kt`: sửa guard tối thiểu — thêm `!currentDestination.hasRoute<Onboarding>()` vào điều kiện LaunchedEffect (cạnh check Login/Register hiện có), KHÔNG đổi gì khác.
- Strings mới đủ EN + VI.

### Phase 9 — Responsive + a11y + audit cuối (M) 🛑 checklist tổng
- Quét landscape TỪNG màn trong §9 trên emulator/preview.
- Audit contrast WCAG AA (bảng cặp màu thực tế), sửa hex nếu fail.
- Verify reduce-motion thực tế (Compose `MotionDurationScale` tự scale theo `animatorDurationScale` — verify claim này; hiệu ứng tự viết loop/auto check thủ công qua `LocalReducedMotion`).
- Audit dead code do refactor tạo ra (orphan imports/components Kanso cũ).
- Hoàn thiện §9 — màn nào chưa xong phải ghi LÝ DO, không im lặng bỏ qua.

## 4B. Checklist DONE từng phase — XONG HẾT mới được qua phase tiếp theo

> Quy tắc cứng: mọi ô của phase hiện tại phải tick (kèm bằng chứng: lệnh đã chạy/số liệu) trước khi mở phase sau. Phase có GATE thì thêm điều kiện: user duyệt trên thiết bị thật.

**Phase 0 DONE khi:**
- [ ] Branch `refactor/ui-ux-redesign` tồn tại, tách từ main mới nhất
- [ ] Baseline ghi nhận: 626 test pass / 0 fail
- [ ] `check-strings-parity.sh` + `check-design-tokens.sh` tồn tại, pre-commit gọi cả hai, chạy thử pass/fail đúng
- [ ] `assembleDebug` pass với đủ dep mới (window-size-class, icons-lucide) + 5 font files + licenses
- [ ] 1 commit Phase 0 sạch (working tree không còn file dở)

**Phase 1 DONE khi:**
- [ ] 6 file theme đúng §3 (Color/Theme/Type/Shape/Dimens/Motion), không còn token Kanso cũ được dùng ở theme
- [ ] Spot-check contrast ≥AA cho 4 cặp chính (onSurface/background, onSurfaceVariant/surface, onPrimary/primary — cả light lẫn dark) bằng tool, ghi số vào commit message hoặc plan
- [ ] Preview typography với chuỗi VI dài nhất trong strings.xml (labelLarge trên nút, displayLarge hero) — không tràn/cắt dấu
- [ ] Build + 626 test pass + lint/ktlint/detekt pass

**Phase 1B DONE khi:**
- [ ] Đủ 13 component: MangaCoverCard, SectionHeader, StatusChip, EmptyState, ErrorState, SkeletonShimmer, AppButton, RatingBadge, StyledDropdownMenu, StyledBadge, AppFilterChip, ErrorMessageBox, LoadingIndicator wrapper
- [ ] Mỗi component có @Preview ≥6 cấu hình (light/dark × VI dài nhất, fontScale 1.3, 320dp, landscape) + content test
- [ ] Haptic helper + `LocalReducedMotion` hoạt động (test đơn vị)
- [ ] Build/test/lint/token-gate pass

**Phase 2 DONE khi:**
- [ ] Research Haze: kết quả ghi vào §2 (API, chi phí, fallback) — trình tại GATE 1
- [ ] Nav pill mới portrait + rail landscape: xoay emulator chuyển đúng, không mất state tab
- [ ] Predictive back bật trong manifest + back gesture chạy mượt thử trên emulator API 34+
- [ ] App icon mới + system splash (core-splashscreen) hiển thị đúng light/dark
- [ ] Build/test/lint/parity/token-gate pass

**Phase 3 DONE khi:**
- [ ] Discover đúng blueprint §3B portrait + landscape; CHỈ dùng component từ thư viện 1B
- [ ] Status bar scrim trên hero (icon status bar đọc được trên cover sáng nhất tìm được)
- [ ] Staggered fade-up chạy + TẮT hẳn khi animator scale = 0 (thử trên emulator)
- [ ] Build/test/lint/parity/token-gate pass; goldens re-record trên PR
- [ ] 🛑 **GATE 1**: user duyệt vertical slice trên thiết bị thật (EN+VI × light+dark, so cạnh reference) + quyết Haze

**Phase 4 DONE khi:**
- [ ] Detail + Review đúng blueprint; parallax hero; landscape 2 cột
- [ ] `Text("All")` → `filter_all_languages` (EN+VI); parity pass
- [ ] DropdownMenu chapter dùng StyledDropdownMenu; ChapterDownloadIndicator đúng spec màu/size/transition
- [ ] Chữ trên scrim đạt AA thử với 3 cover thật (sáng / tối / sặc sỡ)
- [ ] Build/test/lint pass; 🛑 **GATE 2**

**Phase 5 DONE khi:**
- [ ] Bars + sheet style mới (glass nếu GATE 1 duyệt Haze, ngược lại translucent token); alpha thành token
- [ ] Toast save/share → feedback component design system (UI-only, logic giữ nguyên — diff không chạm ViewModel)
- [ ] Cutout landscape verify trên emulator có notch
- [ ] `LoadingIndicator` thay CircularProgressIndicator trong reader; build/test/lint pass

**Phase 6 DONE khi:**
- [ ] Library + Search + FilterSheet đúng blueprint; grid đổi cột theo WindowSizeClass (xoay thử)
- [ ] Empty/error state mọi màn dùng EmptyState/ErrorState chung; Badge/FilterChip đúng spec 1B
- [ ] Build/test/lint/parity/token-gate pass

**Phase 7 DONE khi:**
- [ ] (7a) Settings section card + mục "Xem lại hướng dẫn" hiển thị đúng design system (UI-only, nav để placeholder vì destination `Onboarding` thuộc Phase 8)
- [ ] (7b — tick ở Phase 8) mục "Xem lại hướng dẫn" bind sang destination `Onboarding` thật + reset `reader_hint_done`
- [ ] Auth: ErrorMessageBox + imePadding (bàn phím không che input — thử emulator) + landscape 2 cột
- [ ] Build/test/lint/parity pass; 🛑 **GATE 3**

**Phase 8 DONE khi:**
- [ ] Destination `Onboarding` tạo trong `NavigationDestinations.kt` TRƯỚC, rồi mới sửa guard `MainNavGraph` (tránh compile error vì class chưa tồn tại)
- [ ] 4 kịch bản thủ công pass trên emulator, MỖI kịch bản kèm bằng chứng (screenshot + đọc flag DataStore qua `adb shell` hoặc log): (i) fresh install (`adb uninstall` trước) → thấy carousel; (ii) skip → flag `onboarding_welcome_done=true`, mở lại app không thấy; (iii) user cũ đã login → không thấy carousel, vào Reader lần đầu thấy spotlight; (iv) Settings "Xem lại hướng dẫn" → carousel mở + `reader_hint_done` reset
- [ ] Bind ô (7b) của Phase 7: Settings → Onboarding hoạt động
- [ ] Guard `MainNavGraph` sửa đúng 1 điều kiện (`hasRoute<Onboarding>()`), không đổi flow khác
- [ ] TalkBack đọc được carousel + spotlight (bật TalkBack thử); nút Bỏ qua focusable
- [ ] Strings mới đủ EN+VI, parity pass; câu VI khó đã đưa 2 phương án cho user chọn
- [ ] Build/test/lint pass; 🛑 **GATE 4**

**Phase 9 DONE khi:**
- [ ] Landscape sweep đủ 22 hàng §9 (1, 1b, 1c, 2–20 — từng hàng ghi OK/lý do)
- [ ] Bảng contrast AA đầy đủ mọi cặp màu đang dùng; fix hết cặp fail
- [ ] fontScale 2.0: hero/nút không vỡ (maxLines+ellipsis); touch target ≥48dp; TalkBack sweep các màn chính
- [ ] reduce-motion verify 2 lớp: unit test `LocalReducedMotion = true` → stagger/parallax/shimmer render thẳng final state (automated, chạy được trong suite); cộng kiểm thủ công animator scale 0 trên emulator
- [ ] Dead code audit: 0 orphan import/component Kanso; gỡ `material-icons-extended` nếu grep 0 usage
- [ ] Checklist §9 đủ 22 hàng trạng thái; 🛑 **GATE 5** — đóng dự án

## 5. Onboarding spec (Phase 8)

### Tầng 1 — Welcome carousel (3 trang, destination `Onboarding` trước `Login`)

| Trang | Nội dung (bám tính năng thật) | Minh họa |
|---|---|---|
| 1 | Khám phá hàng nghìn manga từ MangaDex — tìm theo thể loại, ngôn ngữ | vector + cover mock |
| 2 | Lưu vào thư viện, tải chapter đọc offline | vector |
| 3 | Đọc theo cách của bạn — cuộn dọc webtoon hoặc lật ngang, sáng/tối | vector |

- Nút **Bỏ qua** (mọi trang) + **Bắt đầu** (trang cuối) → set `onboarding_welcome_done` → Login.
- Parallax nhẹ illustration↔text theo swipe (tắt khi reduce-motion).
- ⚠️ Đụng guard `LaunchedEffect` trong `MainNavGraph.kt` (điều hướng về Login khi userId null) — ngoại lệ onboarding ĐÃ DUYỆT, sửa tối thiểu: cho phép đứng ở `Onboarding` khi chưa đăng nhập.

### Tầng 2 — Reader spotlight (lần đầu vào Reader)
- Overlay tối + khoét sáng, 2 bước: (1) vùng tap trái/phải lật trang, tap giữa mở menu; (2) nút đổi chế độ đọc. Nút "Bỏ qua" → set `reader_hint_done`.

### Trạng thái & quy tắc
- 2 key DataStore: `onboarding_welcome_done`, `reader_hint_done` (trong `UserPreferencesDataStore` — pattern sẵn có).
- User cũ đã đăng nhập: set `onboarding_welcome_done` im lặng, chỉ thấy reader hint (đã duyệt).
- Xem lại: Settings → "Xem lại hướng dẫn" → mở carousel + reset `reader_hint_done`.
- **i18n**: 100% text qua `appString()` + key ở CẢ `values/` lẫn `values-vi/`. Bản dịch VI viết tự nhiên như người Việt nói; câu không chắc → đề xuất 2 phương án cho user chọn.
- Onboarding dùng đúng token design system, hỗ trợ light/dark; illustration vector dùng token màu (không hardcode) → tự đổi theo theme.
- Code UI onboarding đặt trong `ui/screens/onboarding/` (detekt `ForbiddenImport` cho phép compose ở `ui/screens/**`); logic flag ở DataStore (không import compose).

## 6. Motion plan

| Vị trí | Hiệu ứng | Duration/easing |
|---|---|---|
| Chuyển màn hình | fade-through (fade + scale 0.98→1) | 250ms, M3 emphasized |
| Discover → Detail | shared element cover art (đã wire) | mặc định M3 |
| Detail hero | parallax + scale khi cuộn | theo scroll offset |
| Discover shelf load đầu | staggered fade-up, trễ ~30ms/cover | 250ms |
| Nav pill indicator | morph vị trí spring | spring medium-low stiffness |
| Bookmark/heart | scale spring 1→1.15→1 | spring bouncy |
| Filter chip | shape morph khi chọn (M3 Expressive) | theo MotionScheme |
| Loading | `LoadingIndicator` morphing shapes | mặc định |
| Reader bars | slide + fade (giữ hành vi, tinh chỉnh easing) | 200ms |
| Carousel onboarding | parallax illustration↔text theo swipe | theo swipe |

**Nguyên tắc**: không animation trong scroll path LazyList; mỗi animation phải có chức năng (định hướng/feedback); reduce-motion: Compose tự scale qua `MotionDurationScale` (verify khi implement), hiệu ứng tự viết (parallax, stagger, loop) check thủ công `LocalReducedMotion` (đọc `Settings.Global.ANIMATOR_DURATION_SCALE == 0`) → thay bằng hiển thị tĩnh.

**Deviation Phase 1**: `MotionScheme.expressive()` còn INTERNAL trong material3 1.4.0 (BOM 2026.05.01, compiler xác nhận 2026-06-11). Theme dùng motion mặc định M3 — quyết bump material3 lên 1.5+ (expressive public) tại GATE 1 nếu stable.

## 7. Dark mode — xử lý riêng (không phải đảo màu)

1. Tách lớp: dark = 4 bậc surfaceContainer + tắt `surfaceTint`; light = shadow.
2. Cover art/ảnh trên dark: border `outline` 1dp (cover trắng không "bay" biên) + scrim gradient hero đủ đậm cho text AA trên mọi cover.
3. Illustration onboarding: vector theo token màu → tự đổi theme.
4. Contrast audit Phase 9: bảng cặp màu thực tế × WCAG AA.
5. Status colors: giữ pattern cặp dark riêng, giá trị mới theo palette.

## 7B. Cam kết đồng nhất — cơ chế cưỡng chế (yêu cầu user: "hoàn hảo tuyệt đối, không inconsistency")

| Nguy cơ | Cơ chế chặn (tự động/cấu trúc, không trông vào kỷ luật) |
|---|---|
| Màu lộn xộn, cỡ chữ tùy tiện | Gate `check-design-tokens.sh` ở pre-commit: cấm `Color(0x`/`fontSize =`/`FontFamily.` ngoài `ui/theme/` |
| Mỗi màn vẽ một kiểu | Shared component library (Phase 1B) — màn hình chỉ ghép từ component chung; component mới phải vào thư viện trước khi dùng |
| Vỡ layout, tràn chữ, khoảng trắng thừa | Preview chuẩn 6 cấu hình (light/dark × VI dài nhất/fontScale 1.3/320dp/landscape) → auto-sinh screenshot test; spacing CHỈ từ `Dimens`, không padding chồng padding (review từng PR) |
| Cover lệch tỷ lệ giữa các màn | `MangaCoverCard` là component duy nhất render cover, ratio 2:3 hardcode một chỗ |
| Spacing/section thừa thiếu không đều | Quy tắc: khoảng cách giữa section = `Dimens.space24` duy nhất; trong section = `space12/16`; vi phạm bắt ở review checklist Phase 9 |
| Regression thị giác khi sửa màn khác | 23+ Roborazzi goldens light+dark trên CI — diff 1px là fail |
| Insets lệch (status bar đè chữ, nav bar che nút) | Edge-to-edge contract: mọi screen khai báo xử lý `WindowInsets` rõ ràng; kiểm landscape ở Phase 9 |

Phase 9 chạy **consistency audit cuối**: đặt screenshot mọi màn cạnh nhau (light + dark + 2 ngôn ngữ), soát chéo spacing/màu/type theo bảng trên; lệch = fix trước khi đóng.

## 8. Xác minh & ràng buộc (áp dụng MỌI commit)

- `JAVA_HOME="C:/Program Files/Java/jdk-21.0.10" ./gradlew assembleDebug` pass (cú pháp bash/Git Bash/CI; PowerShell dùng `$env:JAVA_HOME='C:/Program Files/Java/jdk-21.0.10'; ./gradlew assembleDebug`).
- `./gradlew :app:testDebugUnitTest --console=plain` pass (so với baseline Phase 0).
- `./gradlew lintDebug ktlintCheck detekt` pass — không baseline-hóa vi phạm, fix thật.
- `scripts/check-strings-parity.sh` pass — chuỗi UI mới/sửa có mặt CẢ `values/` lẫn `values-vi/`, không ngôn ngữ nào rơi về fallback.
- `scripts/check-design-tokens.sh` pass — không hardcode màu/cỡ chữ/font ngoài `ui/theme/` trong staged files.
- Goldens: re-record qua label `record-screenshots` trên PR (CI Linux = source of truth, KHÔNG record local Windows).
- Coverage 85% diff: screen mới có `@Preview` fake data (auto-sinh screenshot test qua `generateComposePreviewRobolectricTests`) + content test pattern `createAndroidComposeRule<ComponentActivity>`.
- **KHÔNG đụng logic nghiệp vụ/ViewModel/data layer** — ngoại lệ DUY NHẤT: navigation guard Onboarding + 2 key DataStore (§5). Buộc phải đụng logic khác → DỪNG hỏi user.
- Tuyệt đối không động vào `main`, không force-push. Pre-push hook chặn push main.
- Detekt: `ForbiddenImport` (compose chỉ trong `ui/screens|navigation|theme|util`), `UndocumentedPublicFunction` (KDoc cho public function ngoài path miễn trừ).
- Sai 2 lần liên tiếp cùng một chỗ → dừng, báo user.

## 9. Checklist phạm vi (cập nhật sau mỗi phase — trạng thái cuối ở Phase 9)

| # | Màn hình/Component | File chính | Phase | Trạng thái | Ghi chú/Lý do nếu chưa xong |
|---|---|---|---|---|---|
| 1 | Design system | `ui/theme/*` | 1 | ⬜ Chưa | |
| 1b | Shared component library + token gate | `ui/screens/components/`, `scripts/` | 1B | ⬜ Chưa | |
| 1c | App icon + system splash | `res/mipmap*`, `core-splashscreen` | 2 | ⬜ Chưa | |
| 2 | FloatingPillNavBar (+ rail landscape) | `ui/navigation/FloatingPillNavBar.kt` | 2 | ⬜ Chưa | |
| 3 | AuthLoadingScreen → splash | `MainActivity.kt` | 2 | ⬜ Chưa | |
| 4 | Discover | `DiscoverScreen.kt` + Chrome/Shelf | 3 | ⬜ Chưa | |
| 5 | Manga Detail | `MangaDetailScreen.kt` + Hero/Sections/Dimensions | 4 | ⬜ Chưa | |
| 6 | Chapter list | `ChapterComponents.kt` | 4 | ⬜ Chưa | |
| 7 | Manga Review | `MangaReviewScreen.kt` | 4 | ⬜ Chưa | |
| 8 | Reader bars + sheet | `reader/ReaderBars.kt`, `PageActionBottomSheet.kt` | 5 | ⬜ Chưa | |
| 9 | Library | `LibraryScreen.kt` | 6 | ⬜ Chưa | |
| 10 | Search | `SearchScreen.kt` | 6 | ⬜ Chưa | |
| 11 | Search Filter sheet | `SearchFilterSheet.kt` | 6 | ⬜ Chưa | |
| 12 | Settings (+ mục xem lại hướng dẫn) | `SettingScreen.kt` | 7 | ⬜ Chưa | |
| 13 | Login | `auth/LoginScreen.kt` | 7 | ⬜ Chưa | |
| 14 | Register | `auth/RegisterScreen.kt` | 7 | ⬜ Chưa | |
| 15 | Welcome carousel (MỚI) | `ui/screens/onboarding/` | 8 | ⬜ Chưa | |
| 16 | Reader spotlight (MỚI) | `ui/screens/onboarding/` | 8 | ⬜ Chưa | |
| 17 | Responsive pass toàn app | tất cả | 9 | ⬜ Chưa | |
| 18 | Contrast/reduce-motion audit | tất cả | 9 | ⬜ Chưa | |
| 19 | DropdownMenu/Badge/FilterChip/feedback styling | `ChapterComponents`, `DiscoverChrome`, `SearchScreen`, `ReaderEffectHandler` | 1B/4/5 | ⬜ Chưa | |
| 20 | Migrate icon Lucide + gỡ `material-icons-extended` | toàn bộ ui/ | 1B→9 | ⬜ Chưa | gỡ dep khi grep 0 usage |

> Wrapper `ui/screens/detail/MangaDetailScreen.kt` (18 dòng delegate) không tính là màn riêng — đổi theo #5.

## 10. Điểm dừng báo cáo (user kiểm tra bằng mắt, CẢ 2 ngôn ngữ, light + dark)

1. **GATE 1** — sau Phase 3 (vertical slice: design system + component library + nav chrome + Discover hoàn chỉnh). Chốt chuẩn "wow" trên màn thật trước khi nhân rộng; **quyết dependency Haze tại đây**. Phase 1/1B/2 KHÔNG có gate riêng.
2. **GATE 2** — sau Phase 4 (Detail + Review).
3. **GATE 3** — sau Phase 7, kiểm CỤM Phase 5–7 (Reader + Library + Search + FilterSheet + Settings + Auth).
4. **GATE 4** — sau Phase 8 (onboarding).
5. **GATE 5** — sau Phase 9: checklist §9 hoàn chỉnh, màn chưa xong ghi rõ LÝ DO.

Cách kiểm tại MỖI gate: thiết bị thật (≥1 máy), EN+VI × light+dark, đặt cạnh app reference (Crunchyroll/Webtoon); trả lời trung thực "có wow không" — "tạm được" = KHÔNG pass.

Báo cáo tiến độ ghi kèm **giả định đáng chú ý** đã tự quyết (spacing, duration, tên token, câu chữ tutorial...).

## 10B. Backlog sau refactor (feature mới, ngoài scope UI/UX refactor)

| # | Feature | Mô tả | Ưu tiên |
|---|---|---|---|
| B1 | User Profile page | Trang riêng: avatar, thống kê đọc, reading history, achievements | Cao |
| B2 | Reading Statistics / Charts | Chart thống kê: số chapter đọc/tuần, thời gian đọc, genre distribution | Cao |
| B3 | Sign-out confirmation dialog | Cảnh báo trước khi đăng xuất (hiện gọi thẳng, thiếu guard) | Trung bình |
| B4 | Login/Register redesign layout | Brand area + illustration, layout cinematic thay form trơ | Trung bình |
| B5 | Navbar flash on cold start | AuthLoadingScreen → NavHost mount → nav bar hiện rồi ẩn; fix: ẩn nav khi auth=Loading | Trung bình |
| B6 | Download Manager page | Trang quản lý download: xem tiến độ, xóa batch, retry failed | Thấp |
| B7 | Notification center | Trang thông báo: chapter mới, download xong | Thấp |

## 11. Nguồn nghiên cứu (Bước 2)

- Trend thị giác 2025–2026: [Muzli](https://muz.li/blog/whats-changing-in-mobile-app-design-ui-patterns-that-matter-in-2026/), [Tubik](https://blog.tubikstudio.com/ui-design-trends-2026/), [Envato — calm interfaces](https://elements.envato.com/learn/ux-ui-design-trends)
- M3 Expressive: [Android Developers](https://developer.android.com/develop/ui/compose/designsystems/material3), [supercharge.design](https://supercharge.design/blog/material-3-expressive), [What's New in Compose 05/2025](https://android-developers.googleblog.com/2025/05/whats-new-in-jetpack-compose.html)
- Onboarding: [Appcues](https://www.appcues.com/blog/essential-guide-mobile-user-onboarding-ui-ux), [UX Design Institute](https://www.uxdesigninstitute.com/blog/ux-onboarding-best-practices-guide/), [Designerup — 200 onboarding flows](https://designerup.co/blog/i-studied-the-ux-ui-of-over-200-onboarding-flows-heres-everything-i-learned/)
- Adaptive layouts: [Android Developers — adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive), [Compose adaptive stable](https://android-developers.googleblog.com/2024/09/jetpack-compose-apis-for-building-adaptive-layouts-material-guidance-now-stable.html)
- Motion: [acodez](https://acodez.in/micro-interactions-motion-design/), [Design Shack](https://designshack.net/articles/ux-design/microinteractions-mobile-ux/)
- Font: [Bricolage Grotesque chính thức](https://ateliertriay.github.io/bricolage/) (tiếng Việt: cố vấn Nhung Nguyen), [Google Fonts](https://fonts.google.com/specimen/Bricolage%2BGrotesque)
