package com.example.mybookslibrary.util.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
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
    fun quickSave_happyPath_savesFile() = runTest {
        val bytes = pngBytes()
        val picturesRoot = createTempDirectoryFile()
        mockkStatic(Environment::class)
        every { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) } returns picturesRoot
        stubOkHttpSuccess(bytes)

        val uri = ImageSaver(context).quickSave("https://example.com/image", "page_01")

        val expectedFile = File(File(picturesRoot, "MyBooksLibrary"), "page_01.png")
        assertEquals(Uri.fromFile(expectedFile), uri)
        assertTrue(expectedFile.exists())
    }

    @Test
    fun quickSave_networkError_throwsIOException() = runTest {
        stubOkHttpError(IOException("network"))

        assertThrows(IOException::class.java) {
            ImageSaver(context).quickSave("https://example.com/image", "page_01")
        }
    }

    @Test
    fun quickSave_storageError_throwsException() = runTest {
        val bytes = pngBytes()
        mockkStatic(Environment::class)
        every { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) } throws RuntimeException("storage")
        stubOkHttpSuccess(bytes)

        assertThrows(RuntimeException::class.java) {
            ImageSaver(context).quickSave("https://example.com/image", "page_01")
        }
    }

    @Test
    fun saveToUri_happyPath_writesBytes() = runTest {
        val bytes = pngBytes()
        val uri = Uri.parse("content://test/1")
        val output = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns output
        stubOkHttpSuccess(bytes)

        ImageSaver(context).saveToUri("https://example.com/image", uri)

        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test
    fun saveToUri_networkError_throwsIOException() = runTest {
        val uri = Uri.parse("content://test/1")
        stubOkHttpError(IOException("network"))

        assertThrows(IOException::class.java) {
            ImageSaver(context).saveToUri("https://example.com/image", uri)
        }
    }

    @Test
    fun saveToUri_storageError_throwsImageSaveException() = runTest {
        val bytes = pngBytes()
        val uri = Uri.parse("content://test/1")
        every { contentResolver.openOutputStream(uri) } returns null
        stubOkHttpSuccess(bytes)

        assertThrows(ImageSaveException::class.java) {
            ImageSaver(context).saveToUri("https://example.com/image", uri)
        }
    }

    @Test
    fun shareImage_happyPath_returnsIntent() = runTest {
        val bytes = pngBytes()
        val cacheDir = createTempDirectoryFile()
        val expectedUri = Uri.parse("content://provider/shared/page_01.png")
        mockkStatic(FileProvider::class)
        every { context.cacheDir } returns cacheDir
        every { context.packageName } returns "com.example.mybookslibrary"
        every { FileProvider.getUriForFile(context, "com.example.mybookslibrary.provider", any()) } returns expectedUri
        stubOkHttpSuccess(bytes)

        val intent = ImageSaver(context).shareImage("https://example.com/image", "page_01")

        val expectedFile = File(File(cacheDir, "shared_images"), "page_01.png")
        assertTrue(expectedFile.exists())
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals(expectedUri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
    }

    @Test
    fun shareImage_networkError_throwsIOException() = runTest {
        val cacheDir = createTempDirectoryFile()
        every { context.cacheDir } returns cacheDir
        stubOkHttpError(IOException("network"))

        assertThrows(IOException::class.java) {
            ImageSaver(context).shareImage("https://example.com/image", "page_01")
        }
    }

    @Test
    fun shareImage_storageError_throwsException() = runTest {
        val bytes = pngBytes()
        val cacheDir = createTempDirectoryFile()
        mockkStatic(FileProvider::class)
        every { context.cacheDir } returns cacheDir
        every { context.packageName } returns "com.example.mybookslibrary"
        every { FileProvider.getUriForFile(context, "com.example.mybookslibrary.provider", any()) } throws RuntimeException("provider")
        stubOkHttpSuccess(bytes)

        assertThrows(RuntimeException::class.java) {
            ImageSaver(context).shareImage("https://example.com/image", "page_01")
        }
    }

    private fun stubOkHttpSuccess(bytes: ByteArray) {
        val request = Request.Builder().url("https://example.com/image").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(bytes.toResponseBody("image/png".toMediaType()))
            .build()
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns response
    }

    private fun stubOkHttpError(error: IOException) {
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } throws error
    }

    private fun pngBytes(): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
        )
    }

    private fun createTempDirectoryFile(): File {
        return Files.createTempDirectory("imagesaver_test").toFile()
    }
}

