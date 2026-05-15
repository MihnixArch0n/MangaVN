# TRẠNG THÁI DỰ ÁN (STATUS)

## 1. Những phần đã hoàn thành (What has been done)

### Data Layer (Local & Remote)
*   **Room Database:** Đã khởi tạo `AppDatabase` cùng các thực thể chuẩn theo yêu cầu: `UserEntity`, `LibraryItemEntity`, `ChapterProgressEntity` và các Dao tương ứng; `ChapterDao.deleteLibraryItemAndProgress()` đã được bọc trong `@Transaction`.
*   **Local Preferences:** Cài đặt `UserPreferencesDataStore` lưu trữ các cài đặt `reader_quality`, `language`, `theme_mode`.
*   **MangaDex API:** Đã cấu hình Retrofit (`MangaDexApi.kt`) gọi đầy đủ các endpoint:
    *   Lấy danh sách manga (`/manga`).
    *   Tìm kiếm (`/manga` với title query).
    *   Chi tiết manga (`/manga/{id}`).
    *   Lấy danh sách chapters (`/manga/{id}/feed`).
    *   Tải ảnh trang truyện qua server At-Home (`/at-home/server/{chapterId}`).
*   **Image Loading Security:** Có `ImageOkHttpClient` riêng cho Coil và `ImageLoader` độc lập, không gắn header Authorization khi tải ảnh.

### UI & Tính năng các màn hình
*   **Main Flow (4 Tabs):**
    *   **DiscoverScreen:** Hiển thị nổi bật (Spotlight), danh sách Popular, New Releases, Explore. UI đẹp, responsive.
    *   **SearchScreen:** Cho phép nhập từ khóa tìm kiếm và hiển thị danh sách kết quả.
    *   **LibraryScreen:** Liệt kê các truyện đã lưu (Bookmark) kèm theo tiến độ đọc (`READING`, `COMPLETED`, `FAVORITE`). Hỗ trợ Long-press để xoá truyện.
    *   **SettingScreen:** Tích hợp đầy đủ các tuỳ chọn: Đổi Theme, Đổi ngôn ngữ, Chất lượng tải ảnh (Data/Data-saver), Xóa Cache, Backup/Restore database, và nút Đăng xuất.
*   **Navigation:** `MainNavGraph` đã chuyển sang pill bottom bar + `SharedTransitionLayout`, có route riêng cho `MangaDetailScreen`, `ReaderScreen` và `MangaReviewScreen`; route detail/reader dùng tham số đã encode để tránh lỗi URL.
*   **Detail Flow & Reader Flow:**
    *   **MangaDetailScreen:** Hiển thị thông tin truyện, bìa ảnh, tóm tắt và danh sách chapter nhóm theo Volume (Quyển). Chapter list được merge với Room progress để hiện `UNREAD`, `READING`, `COMPLETED`; các chapter unavailable bị lọc bỏ.
    *   **ReaderScreen:** Đọc truyện cuộn dọc mượt mà với `MangaPageItem` riêng để giữ aspect ratio, hỗ trợ chạm để ẩn/hiện thanh công cụ (Overlay), theo dõi `lastReadPage`, khôi phục `startPageIndex`, và tự động lưu tiến độ khi đổi trang/thoát.

---

## 2. Những phần còn thiếu sót / Cần làm tiếp (What needs to be done / Missing)

### Auth Flow (Luồng Đăng nhập)
*   Màn hình Login (LoginScreen): Hiện tại trong code chưa có giao diện `LoginScreen` hay luồng Auth Flow nào. Ứng dụng đang đi thẳng vào `DiscoverScreen` khi khởi chạy, bỏ qua bước kiểm tra đăng nhập.
*   Lưu trạng thái đăng nhập: `UserPreferencesDataStore` hiện đang thiếu cờ (flag) để kiểm tra xem user đã đăng nhập hay chưa (ví dụ: `is_logged_in` hoặc `logged_in_user_id`).
*   Logic User: Đã có `UserEntity` và `UserDao` nhưng chưa có repository hay ViewModel nào xử lý việc đăng ký user mẫu, xác thực user/password mô phỏng khi đăng nhập.

### Known Issue
* Chưa có Login/Auth Flow; app vẫn vào thẳng main flow khi khởi chạy.
