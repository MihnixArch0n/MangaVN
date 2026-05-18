package com.example.mybookslibrary.util.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import timber.log.Timber

/**
 * Downloads manga page images and persists them for save/share workflows.
 *
 * The implementation uses a dedicated [OkHttpClient] with no interceptors so
 * image requests never inherit authentication headers. It also inspects magic
 * bytes to derive the final image format instead of trusting the URL suffix.
 *
 * @param context Application context used to access [android.content.ContentResolver],
 * cache storage, and [FileProvider].
 */
class ImageSaver(private val context: Context) {

    /** Bare OkHttpClient — no auth headers and no logging interceptors. */
    private val httpClient = OkHttpClient.Builder().build()

    /**
     * Downloads [imageUrl], detects its format, and saves it to the device gallery.
     *
     * On Android 10+, the image is written through [MediaStore] into
     * `Pictures/MyBooksLibrary/`. On older devices, it falls back to app-scoped
     * external pictures storage so no legacy storage permission is required.
     *
     * @param imageUrl Full image URL for the manga page.
     * @param displayName Base file name without extension, such as `page_03`.
     * @return The saved content [Uri].
     * @throws Exception when downloading or persisting the image fails.
     */
    fun quickSave(imageUrl: String, displayName: String): Uri {
        Timber.d("quickSave start: displayName=%s, imageUrl=%s", displayName, imageUrl)
        return try {
            val image = downloadImage(imageUrl)
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val filename = "$safeName.${image.format.extension}"

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(image.bytes, filename, image.format.mimeType)
            } else {
                saveToAppExternalPictures(image.bytes, filename, image.format.mimeType)
            }

            Timber.d("quickSave end: displayName=%s, uri=%s", displayName, uri)
            uri
        } catch (e: Exception) {
            Timber.e(e, "quickSave failed: displayName=%s, imageUrl=%s", displayName, imageUrl)
            throw if (e is ImageSaveException) e else ImageSaveException("quickSave failed for $displayName", e)
        }
    }

    /**
     * Downloads [imageUrl] and writes the bytes into a caller-provided SAF [uri].
     *
     * @param imageUrl Full image URL for the manga page.
     * @param uri Destination document [Uri] returned by the Storage Access Framework.
     * @throws Exception when downloading or writing to [uri] fails.
     */
    fun saveToUri(imageUrl: String, uri: Uri) {
        Timber.d("saveToUri start: uri=%s, imageUrl=%s", uri, imageUrl)
        try {
            val bytes = downloadImage(imageUrl).bytes
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
            } ?: throw ImageSaveException("Cannot open output stream for $uri")
            Timber.d("saveToUri end: uri=%s", uri)
        } catch (e: Exception) {
            Timber.e(e, "saveToUri failed: uri=%s, imageUrl=%s", uri, imageUrl)
            throw if (e is ImageSaveException) e else ImageSaveException("saveToUri failed for $uri", e)
        }
    }

    /**
     * Downloads [imageUrl], stores it in cache, and returns a share [Intent].
     *
     * The file is written under `cacheDir/shared_images/` and exposed through
     * [FileProvider] as a safe `content://` URI.
     *
     * @param imageUrl Full image URL for the manga page.
     * @param displayName Base file name without extension.
     * @return An `ACTION_SEND` [Intent] with read permission granted for the file.
     * @throws Exception when downloading, writing, or URI creation fails.
     */
    fun shareImage(imageUrl: String, displayName: String): Intent {
        Timber.d("shareImage start: displayName=%s, imageUrl=%s", displayName, imageUrl)
        try {
            val image = downloadImage(imageUrl)
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val filename = "$safeName.${image.format.extension}"

            val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(shareDir, filename)
            file.writeBytes(image.bytes)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = image.format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Timber.d("shareImage end: displayName=%s, contentUri=%s", displayName, contentUri)
            return intent
        } catch (e: Exception) {
            Timber.e(e, "shareImage failed: displayName=%s, imageUrl=%s", displayName, imageUrl)
            throw if (e is ImageSaveException) e else ImageSaveException("shareImage failed for $displayName", e)
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ────────────────────────────────────────────────────────────────

    private fun downloadImage(url: String): DownloadedImage {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ImageSaveException("Download failed (HTTP ${response.code}): $url")
            }
            val responseBody = response.body ?: throw ImageSaveException("Download response body was empty: $url")
            val bytes = responseBody.bytes()
            if (bytes.isEmpty()) {
                throw ImageSaveException("Downloaded content was empty: $url")
            }
            val format = detectFormat(bytes)
                ?: throw ImageSaveException("Downloaded content is not a supported image: $url")
            return DownloadedImage(bytes, format)
        }
    }

    /**
     * Reads the first bytes of [data] to identify the image format.
     */
    private fun detectFormat(data: ByteArray): ImageFormat? {
        if (data.size < 4) return null

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
            else -> null
        }
    }

    /** MediaStore insertion for Android 10+ (no `WRITE_EXTERNAL_STORAGE` needed). */
    @Suppress("NewApi")
    private fun saveViaMediaStore(
        bytes: ByteArray,
        filename: String,
        mimeType: String
    ): Uri {
        var uri: Uri? = null
        try {
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

            uri = context.contentResolver.insert(collection, values)
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
        } catch (e: Exception) {
            Timber.e(e, "saveViaMediaStore failed: filename=%s, mimeType=%s", filename, mimeType)
            uri?.let {
                try {
                    context.contentResolver.delete(it, null, null)
                } catch (delE: Exception) {
                    Timber.e(delE, "Failed to delete pending URI: %s", it)
                }
            }
            throw if (e is ImageSaveException) e else ImageSaveException("saveViaMediaStore failed", e)
        }
    }

    /** Legacy save for Android 9 and below using app-scoped external storage. */
    private fun saveToAppExternalPictures(
        bytes: ByteArray,
        filename: String,
        mimeType: String
    ): Uri {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw ImageSaveException("External pictures directory unavailable")
        val dir = File(baseDir, "MyBooksLibrary").apply { mkdirs() }
        if (!dir.exists()) throw ImageSaveException("Cannot create pictures directory: ${dir.absolutePath}")

        val file = File(dir, filename)
        file.writeBytes(bytes)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
        return Uri.fromFile(file)
    }

}

private data class DownloadedImage(
    val bytes: ByteArray,
    val format: ImageFormat
)

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
