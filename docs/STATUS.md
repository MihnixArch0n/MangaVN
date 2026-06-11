# TRẠNG THÁI DỰ ÁN (STATUS)

## 1. Những phần đã hoàn thành (What has been done)

### Auth & User Management (Firebase)
*   **Firebase Authentication:** Đã tích hợp Firebase Auth hỗ trợ đăng nhập qua Email/Password và Google Sign-in (sử dụng Credential Manager).
*   **Auth Flow:** Đã hoàn thiện màn hình `LoginScreen`, `RegisterScreen`, luồng điều hướng khởi động dựa trên `AuthStatus` lưu tại DataStore (LOGGED_IN, LOGGED_OUT, GUEST).
*   **Account Management:** Cho phép Xóa tài khoản (Clear Local DB + Clear Firestore Cloud Data + Xóa Firebase User).

### Data Layer (Local & Remote)
*   **Room Database:** Quản lý Bookmark và Tiến độ đọc (`LibraryItemEntity`, `ChapterProgressEntity`). Đã hỗ trợ offline-first, thêm cờ `sync_status`.
*   **Cloud Sync (Firestore):** Đồng bộ 2 chiều qua Firebase Firestore (lưu tại `users/{userId}/library`). Có WorkManager `SyncWorker` chạy ngầm định kỳ và nút Manual Sync.
*   **Local Preferences:** Cài đặt `UserPreferencesDataStore` lưu `reader_quality`, `language`, `theme_mode` và `auth_status`.
*   **MangaDex API:** Retrofit (`MangaDexApi.kt`) gọi endpoint lấy danh sách, chi tiết, tìm kiếm, chapters, hình ảnh.
*   **Image Loading Security:** `ImageOkHttpClient` riêng cho Coil, không đính kèm header Auth khi tải ảnh (tránh bị MangaDex chặn).

### UI & Tính năng các màn hình
*   **Main Flow (4 Tabs):**
    *   **DiscoverScreen:** Hiển thị nổi bật, Popular, New Releases, Explore. UI đẹp, responsive.
    *   **SearchScreen:** Tìm kiếm từ khoá.
    *   **LibraryScreen:** Danh sách truyện Bookmark, lọc bỏ các item bị `PENDING_DELETE`.
    *   **SettingScreen:** Tích hợp Đổi Theme, Ngôn ngữ, Chất lượng tải ảnh, Xóa Cache, Cloud Sync thủ công, Backup/Restore JSON cục bộ, Đăng xuất, và Xóa Tài Khoản.
*   **Navigation:** Pill bottom bar + `SharedTransitionLayout`, có route riêng cho Detail, Reader, Review.
*   **Detail Flow & Reader Flow:**
    *   **MangaDetailScreen:** Hiển thị chi tiết và list chapter gộp chung với Room progress. Ẩn chapter `isUnavailable`.
    *   **ReaderScreen:** Đọc cuộn dọc, theo dõi `lastReadPage`, tự động lưu tiến độ và đánh dấu `PENDING_UPDATE` để SyncWorker đẩy lên Cloud.

---

## 2. Những phần còn thiếu sót / Cần làm tiếp (What needs to be done / Missing)

### Testing
*   Chưa có Unit Test cho `SyncManager` (`LibraryRepository` sync logic), `AuthRepository`, và `UserPreferencesDataStore`.
