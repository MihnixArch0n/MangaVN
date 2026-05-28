package com.example.mybookslibrary.data.remote

import com.example.mybookslibrary.data.remote.models.AtHomeReportRequest
import com.example.mybookslibrary.data.repository.MangaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import timber.log.Timber
import java.io.IOException
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * Reports MangaDex@Home image load telemetry for Coil's OkHttp client.
 *
 * MangaDex asks clients to report page image success/failure for @Home nodes,
 * but not for direct `mangadex.org` hosts. This interceptor measures the full
 * body read duration and byte count, then sends the report on an application
 * I/O scope so image decoding is never blocked by report delivery.
 */
class AtHomeReportInterceptor(
    private val mangaRepository: MangaRepository,
    private val applicationScope: CoroutineScope
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val urlString = url.toString()
        if (request.header(AtHomeReportPolicy.SKIP_REPORT_HEADER) != null) {
            return chain.proceed(
                request.newBuilder()
                    .removeHeader(AtHomeReportPolicy.SKIP_REPORT_HEADER)
                    .build()
            )
        }
        if (!url.isReportableAtHomeImage()) {
            return chain.proceed(request)
        }

        Timber.d("AtHomeReportInterceptor start: url=%s", urlString)
        val startedAt = TimeSource.Monotonic.markNow()
        return try {
            val response = chain.proceed(request)
            val cached = response.header(HEADER_X_CACHE)
                ?.startsWith(CACHE_HIT_PREFIX, ignoreCase = true) == true

            response.newBuilder()
                .body(
                    ReportingResponseBody(
                        delegate = response.body,
                        url = urlString,
                        cached = cached,
                        startedAt = startedAt,
                        initialSuccess = response.isSuccessful,
                        report = ::report
                    )
                )
                .build()
        } catch (ioException: IOException) {
            report(
                url = urlString,
                success = false,
                bytes = 0,
                durationMillis = startedAt.elapsedNow().inWholeMilliseconds,
                cached = false
            )
            Timber.e(ioException, "AtHomeReportInterceptor network failure: url=%s", urlString)
            throw ioException
        }
    }

    private fun report(
        url: String,
        success: Boolean,
        bytes: Long,
        durationMillis: Long,
        cached: Boolean
    ) {
        val request = AtHomeReportRequest(
            url = url,
            success = success,
            bytes = bytes.coerceToInt(),
            duration = durationMillis.coerceAtLeast(0L),
            cached = cached
        )
        Timber.d("AtHomeReportInterceptor report queued: payload=%s", request)
        applicationScope.launch {
            mangaRepository.sendAtHomeReport(request)
        }
    }

    private class ReportingResponseBody(
        private val delegate: ResponseBody,
        private val url: String,
        private val cached: Boolean,
        private val startedAt: TimeSource.Monotonic.ValueTimeMark,
        private val initialSuccess: Boolean,
        private val report: (url: String, success: Boolean, bytes: Long, durationMillis: Long, cached: Boolean) -> Unit
    ) : ResponseBody() {
        private var bytesRead = 0L
        private var completed = false
        private var reported = false
        private val source: BufferedSource by lazy {
            object : ForwardingSource(delegate.source()) {
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val read = super.read(sink, byteCount)
                    if (read == -1L) {
                        completed = true
                        reportOnce()
                    } else {
                        bytesRead += read
                    }
                    return read
                }

                override fun close() {
                    try {
                        super.close()
                    } finally {
                        reportOnce()
                    }
                }
            }.buffer()
        }

        override fun contentLength(): Long = delegate.contentLength()

        override fun contentType(): MediaType? = delegate.contentType()

        override fun source(): BufferedSource = source

        private fun reportOnce() {
            if (reported) return
            reported = true
            val durationMillis = startedAt.elapsedNow().inWholeMilliseconds
            val success = initialSuccess && completed
            Timber.d(
                "AtHomeReportInterceptor body complete: url=%s success=%s bytes=%d duration=%d cached=%s",
                url,
                success,
                bytesRead,
                durationMillis,
                cached
            )
            report(url, success, bytesRead, durationMillis, cached)
        }
    }

    private companion object {
        const val HEADER_X_CACHE = "X-Cache"
        const val CACHE_HIT_PREFIX = "HIT"
    }
}

private fun okhttp3.HttpUrl.isReportableAtHomeImage(): Boolean {
    return AtHomeReportPolicy.isReportableImageUrl(this)
}

private fun Long.coerceToInt(): Int {
    return min(this.coerceAtLeast(0L), Int.MAX_VALUE.toLong()).toInt()
}
