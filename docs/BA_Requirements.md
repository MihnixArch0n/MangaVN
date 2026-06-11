# ĐẶC TẢ YÊU CẦU: MYBOOKSLIBRARY

## 1. Tổng quan Kiến trúc
- **UI Framework:** Kotlin + Jetpack Compose.
- **Navigation:** Jetpack Navigation Compose.
- **Local Database:** Room DB (Lưu lịch sử đọc, yêu thích) - Offline First.
- **Cloud Database:** Firebase Firestore (Đồng bộ thư viện tự động qua WorkManager).
- **Authentication:** Firebase Auth (Email/Password, Google Sign-in) & Guest mode.
- **Local Preferences:** Jetpack DataStore (Lưu AuthStatus, UID, theme, ngôn ngữ, chất lượng đọc ảnh).
- **Network/API:** Retrofit + OkHttp (Call MangaDex API).
- **Image Loading:** Coil.
- **Architecture:** MVVM + Clean Architecture (UI - Domain - Data).

## 2. Thực thể Dữ liệu (Entities & Models)

### 2.1. Local Entities (Room)
* **LibraryItemEntity** (Bookmark/Yêu thích):
  * `manga_id` (PK - map với MangaDex).
  * `title`, `cover_url`.
  * `status` (Enum: `READING`, `COMPLETED`, `FAVORITE`).
  * `last_read_chapter_id`, `last_read_page_index`, `updated_at`.
  * `sync_status` (Enum: `SYNCED`, `PENDING_UPDATE`, `PENDING_DELETE`).
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
* **FirestoreLibraryItem:** DTO đồng bộ Cloud (chứa thông tin thư viện + serverUpdatedAt).

## 3. Phân định Logic
- **MangaDex API:** Lấy danh sách truyện (Discover), tìm kiếm (Search), chi tiết truyện, và tải ảnh trang truyện.
- **Firebase / Room:** Xác thực người dùng (Firebase Auth). Quản lý dữ liệu thư viện đa thiết bị thông qua Firestore, lưu cache dưới Room DB (Offline First) và đồng bộ qua `SyncWorker`.

## 4. Luồng Điều hướng (Navigation Flow)

**A. Auth Flow**
- Khởi chạy -> Kiểm tra `AuthStatus` từ DataStore:
  - Nếu `LOGGED_OUT` hoặc null: Hiện `LoginScreen` -> Đăng nhập bằng Email/Google hoặc "Tiếp tục với tư cách Khách" -> Lưu trạng thái -> Chuyển sang Main Flow.
  - Nếu `LOGGED_IN` hoặc `GUEST`: Chuyển thẳng sang Main Flow.

**B. Main Flow (Bottom Navigation)**
1.  **Discover Tab (`DiscoverScreen`):** Gọi API lấy list truyện -> Click mở `MangaDetailScreen`.
2.  **Search Tab (`SearchScreen`):** Nhập keyword -> Gọi API tìm kiếm -> Hiển thị list -> Click mở `MangaDetailScreen`.
3.  **My Library Tab (`LibraryScreen`):** Hiển thị danh sách dọc các truyện đã Bookmark (query từ `LibraryItemEntity` với `sync_status != PENDING_DELETE`). Click chuyển sang `MangaDetailScreen`.
4.  **User Setting Tab (`SettingScreen`):**
  - Cấu hình giao diện (`theme_mode`) và ngôn ngữ (`language`).
  - Cấu hình tải ảnh (`READER_QUALITY`): Chất lượng Gốc (`data`) hoặc Tiết kiệm (`data-saver`).
  - Xóa Cache (Coil).
  - Cloud Sync (Thủ công) & Backup/Restore (Export/Import JSON cục bộ).
  - Đăng xuất / Xóa tài khoản (Clear toàn bộ dữ liệu Room và Firestore).

**C. Detail & Reader flow**
- **`MangaDetailScreen`**: Hiện mô tả, ảnh, và **danh sách chapter**. Nhóm chapter theo Volume, ẩn chapter `isUnavailable`. Gộp chapter với `ChapterProgressEntity` từ Room để hiển thị tiến độ.
- **`ReaderScreen`**: Tải ảnh qua MangaDex at-home server (độc lập `OkHttpClient`, không mang auth header). Cập nhật Room (`LibraryItemEntity` và `ChapterProgressEntity`) khi đổi trang/thoát màn hình, tự động gắn cờ `PENDING_UPDATE` để `SyncWorker` đẩy lên Firestore.
