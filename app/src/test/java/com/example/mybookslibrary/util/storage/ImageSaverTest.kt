package com.example.mybookslibrary.util.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageSaverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var call: Call

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        okHttpClient = mockk()
        call = mockk()

        every { context.contentResolver } returns contentResolver

        mockkConstructor(OkHttpClient.Builder::class)
        every { anyConstructed<OkHttpClient.Builder>().build() } returns okHttpClient
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun quickSave_happyPath_savesFile() =
        runTest {
            val bytes = pngBytes()
            val picturesRoot = createTempDirectoryFile()
            every { context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) } returns picturesRoot
            stubOkHttpSuccess(bytes)

            val uri = ImageSaver(context).quickSave("https://example.com/image", "page_01")

            val expectedFile = File(File(picturesRoot, "MyBooksLibrary"), "page_01.png")
            assertEquals(Uri.fromFile(expectedFile), uri)
            assertTrue(expectedFile.exists())
        }

    @Test
    fun quickSave_networkError_throwsImageSaveException() =
        runTest {
            stubOkHttpError(IOException("network"))

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).quickSave("https://example.com/image", "page_01")
            }
        }

    @Test
    fun quickSave_storageError_throwsImageSaveException() =
        runTest {
            val bytes = pngBytes()
            every {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            } throws RuntimeException("storage")
            stubOkHttpSuccess(bytes)

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).quickSave("https://example.com/image", "page_01")
            }
        }

    @Test
    @Config(sdk = [29])
    fun quickSave_api29_usesMediaStorePendingFlow() =
        runTest {
            val bytes = pngBytes()
            val inserted = Uri.parse("content://media/external_primary/images/media/42")
            val output = ByteArrayOutputStream()
            every { contentResolver.insert(any(), any()) } returns inserted
            every { contentResolver.openOutputStream(inserted) } returns output
            every { contentResolver.update(inserted, any(), null, null) } returns 1
            stubOkHttpSuccess(bytes)

            val uri = ImageSaver(context).quickSave("https://example.com/image", "page_01")

            assertEquals(inserted, uri)
            assertArrayEquals(bytes, output.toByteArray())
        }

    @Test
    fun saveToUri_happyPath_writesBytes() =
        runTest {
            val bytes = pngBytes()
            val uri = Uri.parse("content://test/1")
            val output = ByteArrayOutputStream()
            every { contentResolver.openOutputStream(uri) } returns output
            stubOkHttpSuccess(bytes)

            ImageSaver(context).saveToUri("https://example.com/image", uri)

            assertArrayEquals(bytes, output.toByteArray())
        }

    @Test
    fun saveToUri_networkError_throwsImageSaveException() =
        runTest {
            val uri = Uri.parse("content://test/1")
            stubOkHttpError(IOException("network"))

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).saveToUri("https://example.com/image", uri)
            }
        }

    @Test
    fun saveToUri_storageError_throwsImageSaveException() =
        runTest {
            val bytes = pngBytes()
            val uri = Uri.parse("content://test/1")
            every { contentResolver.openOutputStream(uri) } returns null
            stubOkHttpSuccess(bytes)

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).saveToUri("https://example.com/image", uri)
            }
        }

    @Test
    fun shareImage_happyPath_returnsIntent() =
        runTest {
            val bytes = pngBytes()
            val cacheDir = createTempDirectoryFile()
            val expectedUri = Uri.parse("content://provider/shared/page_01.png")
            mockkStatic(FileProvider::class)
            every { context.cacheDir } returns cacheDir
            every { context.packageName } returns "com.example.mybookslibrary"
            every {
                FileProvider.getUriForFile(context, "com.example.mybookslibrary.provider", any())
            } returns expectedUri
            stubOkHttpSuccess(bytes)

            val intent = ImageSaver(context).shareImage("https://example.com/image", "page_01")

            val expectedFile = File(File(cacheDir, "shared_images"), "page_01.png")
            assertTrue(expectedFile.exists())
            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("image/png", intent.type)
            @Suppress("DEPRECATION") // getParcelableExtra deprecated API 33; compat helper không có trong test scope
            assertEquals(expectedUri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        }

    @Test
    fun shareImage_networkError_throwsImageSaveException() =
        runTest {
            val cacheDir = createTempDirectoryFile()
            every { context.cacheDir } returns cacheDir
            stubOkHttpError(IOException("network"))

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).shareImage("https://example.com/image", "page_01")
            }
        }

    @Test
    fun shareImage_storageError_throwsImageSaveException() =
        runTest {
            val bytes = pngBytes()
            val cacheDir = createTempDirectoryFile()
            mockkStatic(FileProvider::class)
            every { context.cacheDir } returns cacheDir
            every { context.packageName } returns "com.example.mybookslibrary"
            every {
                FileProvider.getUriForFile(context, "com.example.mybookslibrary.provider", any())
            } throws RuntimeException("provider")
            stubOkHttpSuccess(bytes)

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).shareImage("https://example.com/image", "page_01")
            }
        }

    @Test
    fun quickSave_and_shareImage_detectJpegFormat() =
        runTest {
            assertQuickSaveAndShareFormat(
                bytes = jpegBytes(),
                expectedExt = "jpg",
                expectedMime = "image/jpeg",
            )
        }

    @Test
    fun quickSave_and_shareImage_detectWebpFormat() =
        runTest {
            assertQuickSaveAndShareFormat(
                bytes = webpBytes(),
                expectedExt = "webp",
                expectedMime = "image/webp",
            )
        }

    @Test
    fun quickSave_and_shareImage_detectGifFormat() =
        runTest {
            assertQuickSaveAndShareFormat(
                bytes = gifBytes(),
                expectedExt = "gif",
                expectedMime = "image/gif",
            )
        }

    @Test
    fun quickSave_rejectsEmptyResponseBody() =
        runTest {
            every {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            } returns createTempDirectoryFile()
            stubOkHttpSuccess(ByteArray(0), "text/html")

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).quickSave("https://example.com/image", "page_01")
            }
        }

    @Test
    fun shareImage_rejectsUnsupportedImageBytes() =
        runTest {
            val cacheDir = createTempDirectoryFile()
            every { context.cacheDir } returns cacheDir
            stubOkHttpSuccess("not an image".toByteArray(), "text/html")

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).shareImage("https://example.com/image", "page_01")
            }
        }

    private fun assertQuickSaveAndShareFormat(
        bytes: ByteArray,
        expectedExt: String,
        expectedMime: String,
    ) {
        val picturesRoot = createTempDirectoryFile()
        val cacheDir = createTempDirectoryFile()
        val expectedUri = Uri.parse("content://provider/shared/any.$expectedExt")
        val capturedFile = slot<File>()
        every { context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) } returns picturesRoot
        every { context.cacheDir } returns cacheDir
        every { context.packageName } returns "com.example.mybookslibrary"
        mockkStatic(FileProvider::class)
        every {
            FileProvider.getUriForFile(context, "com.example.mybookslibrary.provider", capture(capturedFile))
        } returns expectedUri
        stubOkHttpSuccess(bytes, expectedMime)

        val quickSaveUri = ImageSaver(context).quickSave("https://example.com/image", "page_01")
        val shareIntent = ImageSaver(context).shareImage("https://example.com/image", "page_01")

        assertTrue(quickSaveUri.toString().endsWith(".$expectedExt"))
        assertEquals(expectedMime, shareIntent.type)
        assertTrue(capturedFile.captured.name.endsWith(".$expectedExt"))
    }

    @Test
    fun quickSave_httpErrorResponse_throwsImageSaveException() =
        // Covers line 145: throw khi response.isSuccessful == false (HTTP 404)
        runTest {
            every { okHttpClient.newCall(any()) } returns call
            every { call.execute() } answers {
                val req = Request.Builder().url("https://example.com/image").build()
                Response
                    .Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .build()
            }

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).quickSave("https://example.com/image", "page_01")
            }
        }

    @Test
    fun quickSave_externalDirNull_throwsImageSaveException() =
        // Covers line 265: ?: throw khi getExternalFilesDir trả về null
        runTest {
            val bytes = pngBytes()
            stubOkHttpSuccess(bytes)
            every { context.getExternalFilesDir(any()) } returns null

            assertThrows(ImageSaveException::class.java) {
                ImageSaver(context).quickSave("https://example.com/image", "page_01")
            }
        }

    private fun stubOkHttpSuccess(
        bytes: ByteArray,
        mimeType: String = "image/png",
    ) {
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } answers {
            val request =
                Request
                    .Builder()
                    .url("https://example.com/image")
                    .build()
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(bytes.toResponseBody(mimeType.toMediaType()))
                .build()
        }
    }

    private fun stubOkHttpError(error: IOException) {
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } throws error
    }

    private fun pngBytes() =
        byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
            0x00,
            0x00,
            0x00,
            0x0D,
        )

    private fun jpegBytes() =
        byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
            0x00,
            0x10,
            0x4A,
            0x46,
        )

    private fun webpBytes() =
        byteArrayOf(
            0x52,
            0x49,
            0x46,
            0x46,
            0x2A,
            0x00,
            0x00,
            0x00,
            0x57,
            0x45,
            0x42,
            0x50,
        )

    private fun gifBytes() =
        byteArrayOf(
            0x47,
            0x49,
            0x46,
            0x38,
            0x39,
            0x61,
            0x00,
            0x00,
        )

    private fun createTempDirectoryFile() = Files.createTempDirectory("imagesaver_test").toFile()
}
