package com.example.mybookslibrary.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phủ Room [LibraryStatusConverters]: round-trip enum<->String và nhánh fallback
 * khi giá trị lưu trong DB không khớp enum nào (vd schema cũ) -> mặc định READING.
 */
class LibraryStatusConvertersTest {
    private val converters = LibraryStatusConverters()

    @Test
    fun fromStatus_traVeTenEnum() {
        assertEquals("READING", converters.fromStatus(LibraryStatus.READING))
        assertEquals("COMPLETED", converters.fromStatus(LibraryStatus.COMPLETED))
    }

    @Test
    fun toStatus_giaTriHopLe_parseDung() {
        assertEquals(LibraryStatus.READING, converters.toStatus("READING"))
        assertEquals(LibraryStatus.COMPLETED, converters.toStatus("COMPLETED"))
    }

    @Test
    fun toStatus_giaTriLa_fallbackReading() {
        // Nhánh catch IllegalArgumentException -> READING
        assertEquals(LibraryStatus.READING, converters.toStatus("UNKNOWN_VALUE"))
        assertEquals(LibraryStatus.READING, converters.toStatus(""))
        // "FAVORITE" legacy (enum đã xóa) cũng rơi vào fallback — cờ is_favorite là cột riêng
        assertEquals(LibraryStatus.READING, converters.toStatus("FAVORITE"))
    }
}
