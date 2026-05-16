package com.example.mybookslibrary.domain.model

/**
 * A stateless utility that maps a horizontal tap coordinate to a [ReaderTapAction]
 * based on the current [ReadingMode].
 *
 * ## Zone Layout (percentage of total width)
 * ```
 * |--- Left (0-25%) ---|--- Center (25-75%) ---|--- Right (75-100%) ---|
 * ```
 *
 * ## Action Mapping
 * | Zone   | LTR            | RTL            | VERTICAL       |
 * |--------|----------------|----------------|----------------|
 * | Left   | PREVIOUS_PAGE  | NEXT_PAGE      | TOGGLE_OVERLAY |
 * | Center | TOGGLE_OVERLAY | TOGGLE_OVERLAY | TOGGLE_OVERLAY |
 * | Right  | NEXT_PAGE      | PREVIOUS_PAGE  | TOGGLE_OVERLAY |
 *
 * In [ReadingMode.VERTICAL], all taps return [ReaderTapAction.TOGGLE_OVERLAY]
 * because page navigation is handled by scrolling, not taps.
 */
object TapZoneEvaluator {

    /** Left zone boundary: 25% of the total width. */
    private const val LEFT_ZONE_END_RATIO = 0.25f

    /** Right zone boundary: 75% of the total width. */
    private const val RIGHT_ZONE_START_RATIO = 0.75f

    /**
     * Evaluates a tap at horizontal coordinate [x] against the [totalWidth]
     * of the reader layout and returns the appropriate [ReaderTapAction].
     *
     * @param x The horizontal tap coordinate in pixels.
     * @param totalWidth The total width of the reader layout in pixels.
     * @param mode The current [ReadingMode].
     * @return The [ReaderTapAction] corresponding to the tap zone and reading mode.
     *         Returns [ReaderTapAction.NONE] if [totalWidth] is not positive.
     */
    fun evaluateTap(x: Float, totalWidth: Float, mode: ReadingMode): ReaderTapAction {
        // Guard: invalid width makes zone calculation meaningless
        if (totalWidth <= 0f) return ReaderTapAction.NONE

        // Clamp x to valid range to handle edge touches
        val clampedX = x.coerceIn(0f, totalWidth)
        val ratio = clampedX / totalWidth

        // VERTICAL mode: tap zones are not used; always toggle overlay
        if (mode == ReadingMode.VERTICAL) return ReaderTapAction.TOGGLE_OVERLAY

        return when {
            // Left zone: [0%, 25%)
            ratio < LEFT_ZONE_END_RATIO -> when (mode) {
                ReadingMode.LTR -> ReaderTapAction.PREVIOUS_PAGE
                ReadingMode.RTL -> ReaderTapAction.NEXT_PAGE
                ReadingMode.VERTICAL -> ReaderTapAction.TOGGLE_OVERLAY // unreachable, covered above
            }
            // Right zone: [75%, 100%]
            ratio >= RIGHT_ZONE_START_RATIO -> when (mode) {
                ReadingMode.LTR -> ReaderTapAction.NEXT_PAGE
                ReadingMode.RTL -> ReaderTapAction.PREVIOUS_PAGE
                ReadingMode.VERTICAL -> ReaderTapAction.TOGGLE_OVERLAY
            }
            // Center zone: [25%, 75%)
            else -> ReaderTapAction.TOGGLE_OVERLAY
        }
    }
}
