package com.example.mybookslibrary.util.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

/**
 * Handles saving and sharing manga page images.
 *
 * ## Design decisions
 * - Uses a **dedicated [OkHttpClient]** with zero interceptors to avoid leaking
 *   auth tokens to MangaDex@Home image servers (see rule: Coil Security).
 * - Determines the correct MIME type and file extension dynamically by reading
 *   the first bytes (magic bytes) of the image stream instead of relying on the URL.
 * - Uses **MediaStore** for gallery saves (Android 10+) and raw file I/O for
 *   older devices — no `WRITE_EXTERNAL_STORAGE` permission required.
 *
 * @param context Application context used for ContentResolver and cache directory access.
 */
class ImageSaver(private val context: Context) {

    /** Bare OkHttpClient — NO auth headers, NO logging interceptors. */
    private val httpClient = OkHttpClient.Builder().build()

    /**
     * Downloads the image at [imageUrl] and saves it to the device gallery
     * under `Pictures/MyBooksLibrary/`.
     *
     * @param imageUrl Full URL of the manga page image.
     * @param displayName Base filename without extension (e.g. "page_03").
     * @return The content [Uri] of the saved image.
     * @throws ImageSaveException if download or save fails.
     */
    suspend fun quickSave(imageUrl: String, displayName: String): Uri {
        Log.d(TAG, "quickSave start: $displayName from $imageUrl")
        val bytes = downloadBytes(imageUrl)
        val format = detectFormat(bytes)
        val filename = "$displayName.${format.extension}"

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(bytes, filename, format.mimeType)
        } else {
            saveToExternalPictures(bytes, filename)
        }

        Log.d(TAG, "quickSave success: $uri")
        return uri
    }

    /**
     * Downloads the image at [imageUrl] and writes it to the caller-provided [uri]
     * obtained from the Storage Access Framework (SAF `CreateDocument`).
     *
     * @param imageUrl Full URL of the manga page image.
     * @param uri Destination URI from SAF.
     * @throws ImageSaveException if download or write fails.
     */
    suspend fun saveToUri(imageUrl: String, uri: Uri) {
        Log.d(TAG, "saveToUri start: $uri from $imageUrl")
        val bytes = downloadBytes(imageUrl)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
        } ?: throw ImageSaveException("Cannot open output stream for $uri")
        Log.d(TAG, "saveToUri success: $uri")
    }

    /**
     * Downloads the image at [imageUrl], caches it in `cacheDir/shared_images/`,
     * and returns a share [Intent] ready to be wrapped in `Intent.createChooser()`.
     *
     * Uses [FileProvider] to generate a safe `content://` URI.
     *
     * @param imageUrl Full URL of the manga page image.
     * @param displayName Base filename without extension.
     * @return A fully configured `ACTION_SEND` [Intent].
     * @throws ImageSaveException if download or cache write fails.
     */
    suspend fun shareImage(imageUrl: String, displayName: String): Intent {
        Log.d(TAG, "shareImage start: $displayName from $imageUrl")
        val bytes = downloadBytes(imageUrl)
        val format = detectFormat(bytes)
        val filename = "$displayName.${format.extension}"

        // Write to cacheDir/shared_images/
        val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(shareDir, filename)
        file.writeBytes(bytes)

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.also {
            Log.d(TAG, "shareImage success: contentUri=$contentUri")
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ────────────────────────────────────────────────────────────────

    private fun downloadBytes(url: String): ByteArray {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw ImageSaveException("Download failed (HTTP ${response.code}): $url")
        }
        return response.body.bytes()
            ?: throw ImageSaveException("Empty response body: $url")
    }

    /**
     * Reads the first bytes of [data] to identify the image format.
     * Falls back to JPEG if unrecognized.
     */
    private fun detectFormat(data: ByteArray): ImageFormat {
        if (data.size < 4) return ImageFormat.JPEG

        return when {
            // PNG: 89 50 4E 47
            data[0] == 0x89.toByte() && data[1] == 0x50.toByte() &&
                data[2] == 0x4E.toByte() && data[3] == 0x47.toByte() -> ImageFormat.PNG
            // WebP: RIFF....WEBP
            data.size >= 12 &&
                data[0] == 0x52.toByte() && data[1] == 0x49.toByte() &&
                data[2] == 0x46.toByte() && data[3] == 0x46.toByte() &&
                data[8] == 0x57.toByte() && data[9] == 0x45.toByte() &&
                data[10] == 0x42.toByte() && data[11] == 0x50.toByte() -> ImageFormat.WEBP
            // JPEG: FF D8 FF
            data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() -> ImageFormat.JPEG
            // GIF: 47 49 46
            data[0] == 0x47.toByte() && data[1] == 0x49.toByte() &&
                data[2] == 0x46.toByte() -> ImageFormat.GIF
            else -> ImageFormat.JPEG
        }
    }

    /** MediaStore insertion for Android 10+ (no WRITE_EXTERNAL_STORAGE needed). */
    private fun saveViaMediaStore(
        bytes: ByteArray,
        filename: String,
        mimeType: String
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/MyBooksLibrary"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )

        val uri = context.contentResolver.insert(collection, values)
            ?: throw ImageSaveException("MediaStore insert returned null")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
        } ?: throw ImageSaveException("Cannot open MediaStore output stream")

        // Clear IS_PENDING so the image becomes visible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }

    /** Legacy save for Android 9 and below. */
    @Suppress("DEPRECATION")
    private fun saveToExternalPictures(bytes: ByteArray, filename: String): Uri {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyBooksLibrary"
        ).apply { mkdirs() }

        val file = File(dir, filename)
        file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    companion object {
        private const val TAG = "ImageSaver"
    }
}

/** Image format with associated MIME type and file extension. */
enum class ImageFormat(val mimeType: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    GIF("image/gif", "gif")
}

/** Exception thrown by [ImageSaver] operations. */
class ImageSaveException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
