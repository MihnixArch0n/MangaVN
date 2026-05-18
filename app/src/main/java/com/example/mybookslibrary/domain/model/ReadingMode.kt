package com.example.mybookslibrary.domain.model

/**
 * Defines the available reading modes for the manga reader.
 *
 * - [LTR]: Horizontal pagination, swiping right-to-left to advance pages.
 * - [RTL]: Horizontal pagination, swiping left-to-right to advance pages (standard manga reading order).
 * - [VERTICAL]: Traditional vertical scrolling (webtoon-style).
 */
enum class ReadingMode {
    LTR,
    RTL,
    VERTICAL
}
