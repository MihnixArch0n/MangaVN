# ĐẶC TẢ YÊU CẦU: MYBOOKSLIBRARY (LOCAL-FIRST)

## 1. Tổng quan Kiến trúc
- **UI Framework:** Kotlin + Jetpack Compose.
- **Navigation:** Jetpack Navigation Compose.
- **Local Database:** Room DB (Lưu user profile ảo, lịch sử đọc, yêu thích).
- **Local Preferences:** Jetpack DataStore (Lưu trạng thái đăng nhập, theme).
- **Network/API:** Retrofit + OkHttp (Call MangaDex API).
- **Image Loading:** Coil.
- **Architecture:** MVVM + Clean Architecture (UI - Domain - Data).

## 2. Thực thể Dữ liệu (Entities & Models)

### 2.1. Local Entities (Room Database)
* **UserEntity:** `id` (PK), `username`, `password` (mô phỏng), `avatar_path`, `created_at`.
* **LibraryItemEntity** (Quản lý sách được Bookmark/Yêu thích):
  * `manga_id` (PK - map với ID từ MangaDex).
  * `title`, `cover_url` (Lưu đệm để hiển thị nhanh).
  * `added_at` (Thời gian thêm vào thư viện để sắp xếp).
* **ChapterProgressEntity** (Quản lý tiến độ đọc chi tiết của từng chapter):
  * `chapter_id` (PK - map với ID chapter từ MangaDex).
  * `manga_id` (FK map với LibraryItemEntity).
  * `status` (Enum: UNREAD, READING, COMPLETED).
  * `last_read_page` (Vị trí trang đang đọc dở).
  * `total_pages` (Tổng số trang của chapter).
  * `updated_at`.

### 2.2. Remote Models (MangaDex API)
*(Chỉ dùng để parse JSON trả về, không lưu vào Room)*
* **MangaModel:** `id`, `title`, `description`, `cover_art`, `rating`, `tags/genres`.
* **ChapterModel:** `id`, `manga_id`, `chapter_number`, `title`, `pages` (List URL ảnh).

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
2.  **Search Tab (`SearchScreen`):** Nhập keyword -> Gọi API tìm kiếm -> Hiển thị list (title, cover, rating) -> Click mở `MangaDetailScreen`.
3.  **My Library Tab (`LibraryScreen`):** Hiển thị danh sách dọc các truyện đã Bookmark (query từ bảng `LibraryItemEntity`). Click vào truyện sẽ chuyển sang `MangaDetailScreen` để xem danh sách chapter. Long-press để xoá truyện khỏi thư viện (xoá luôn tiến độ chapter kèm theo).
4.  **User Setting Tab (`SettingScreen`):** Hiển thị Info User ảo -> Các chức năng:
  - Cấu hình tải ảnh (`READER_QUALITY`): Cho phép chọn chất lượng Gốc (`data`) hoặc Tiết kiệm (`data-saver`). Lưu trữ bằng DataStore (Mặc định: `data`).
  - Xóa Cache (Coil).
  - Backup / Restore dữ liệu (Export/Import Local Database & Preferences).
  - Đăng xuất (Clear DataStore, quay về Auth Flow).

**C. Detail & Reader Flow**
- **`MangaDetailScreen`:** Hiện mô tả, ảnh, và **danh sách chapter**.
  - Gọi API MangaDex (`/feed`) lấy danh sách chapter, gộp (merge) với `ChapterProgressEntity` từ Room để hiển thị trạng thái UI: Chưa đọc, Đang đọc (hiện % tiến độ), Hoàn thành (icon check).
  - UI nhóm các chapter theo Volume (Quyển). Ẩn các chapter không khả dụng (`isUnavailable: true`).
- **`ReaderScreen`:** Hiện trang truyện của 1 chapter. Khi đọc hoặc vuốt đến trang cuối, tự động cập nhật/upsert `last_read_page` và `status` (COMPLETED) vào `ChapterProgressEntity` xuống DB.