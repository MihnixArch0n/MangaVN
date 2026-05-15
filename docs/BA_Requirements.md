# ĐẶC TẢ YÊU CẦU: MYBOOKSLIBRARY (LOCAL-FIRST)

## 1. Tổng quan Kiến trúc
- **UI Framework:** Kotlin + Jetpack Compose.
- **Navigation:** Jetpack Navigation Compose.
- **Local Database:** Room DB (Lưu user profile ảo, lịch sử đọc, yêu thích).
- **Local Preferences:** Jetpack DataStore (Lưu trạng thái đăng nhập, theme, ngôn ngữ, chất lượng đọc ảnh).
- **Network/API:** Retrofit + OkHttp (Call MangaDex API).
- **Image Loading:** Coil.
- **Architecture:** MVVM + Clean Architecture (UI - Domain - Data).

## 2. Thực thể Dữ liệu (Entities & Models)

### 2.1. Local Entities (Room)
* **UserEntity:** `id` (PK), `username`, `password` (mô phỏng), `avatar_path`, `created_at`.
* **LibraryItemEntity** (Bookmark/Yêu thích):
  * `manga_id` (PK - map với MangaDex).
  * `title`, `cover_url`.
  * `status` (Enum: `READING`, `COMPLETED`, `FAVORITE`).
  * `last_read_chapter_id`, `last_read_page_index`, `updated_at`.
* **ChapterProgressEntity** (Tiến độ đọc chapter):
  * `chapter_id` (PK - map với MangaDex Chapter ID).
  * `manga_id` (FK map với `LibraryItemEntity`).
  * `status` (Enum: `UNREAD`, `READING`, `COMPLETED`).
  * `last_read_page` (Vị trí trang đang đọc dở).
  * `total_pages` (Tổng số trang của chapter).
  * `updated_at`.

### 2.2. Domain / Remote Models
* **MangaModel:** `id`, `title`, `description`, `coverArt`, `tags`.
* **ChapterModel:** `id`, `mangaId`, `volume`, `chapterNumber`, `title`, `pages`, `isUnavailable`.
* **ChapterWithProgressModel:** `chapterId`, `mangaId`, `volume`, `chapterNumber`, `title`, `status`, `lastReadPage`, `totalPages`.

## 3. Phân định Logic
- **MangaDex API:** Lấy danh sách truyện (Discover), tìm kiếm (Search), chi tiết truyện, và tải ảnh trang truyện.
- **Room Database:** Xác thực người dùng (Auth) và quản lý dữ liệu cá nhân (Library).

## 4. Luồng Điều hướng (Navigation Flow)

**A. Auth Flow**
- Khởi chạy -> Kiểm tra DataStore:
  - Nếu chưa đăng nhập: Hiện `LoginScreen` -> Nhập user/pass -> Lưu DB -> Chuyển sang Main Flow.
  - Nếu đã đăng nhập: Chuyển thẳng sang Main Flow.

**B. Main Flow (Bottom Navigation)**
1.  **Discover Tab (`DiscoverScreen`):** Gọi API lấy list truyện -> Click mở `MangaDetailScreen`.
2.  **Search Tab (`SearchScreen`):** Nhập keyword -> Gọi API tìm kiếm -> Hiển thị list (title, cover, tags) -> Click mở `MangaDetailScreen`.
3.  **My Library Tab (`LibraryScreen`):** Hiển thị danh sách dọc các truyện đã Bookmark (query từ bảng `LibraryItemEntity`). Click vào truyện sẽ chuyển sang `MangaDetailScreen` để xem danh sách chapter. Long-press để xoá truyện khỏi thư viện (xoá luôn tiến độ chapter kèm theo).
4.  **User Setting Tab (`SettingScreen`):** Hiển thị Info User ảo -> Các chức năng:
  - Cấu hình giao diện (`theme_mode`) và ngôn ngữ (`language`).
  - Cấu hình tải ảnh (`READER_QUALITY`): Cho phép chọn chất lượng Gốc (`data`) hoặc Tiết kiệm (`data-saver`). Lưu trữ bằng DataStore (Mặc định: `data`).
  - Xóa Cache (Coil).
  - Backup / Restore dữ liệu (Export/Import Local Database & Preferences).
  - Đăng xuất (Clear DataStore, quay về Auth Flow).

**C. Detail & Reader flow**
- **`MangaDetailScreen`**: Hiện mô tả, ảnh, và **danh sách chapter**.
- Gọi API MangaDex (`/feed`) lấy danh sách chapter, gộp (merge) với `ChapterProgressEntity` từ Room để hiển thị trạng thái UI: `UNREAD` / `READING` / `COMPLETED`.
- UI nhóm các chapter theo Volume (Quyển). Ẩn các chapter không khả dụng (`isUnavailable: true`).
- **`ReaderScreen`**: nhận `mangaId`, `chapterId`, `chapterTitle`, `startPageIndex`; tải ảnh chapter qua MangaDex at-home server, dùng `MangaPageItem` để giữ aspect ratio, và lưu tiến độ đọc vào Room khi đổi trang/thoát màn.
