package com.example.mybookslibrary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shape tokens (refactor-ui-ux.md §3.3): small 8 / medium 12 / large 16 / extraLarge 24.
// Pill (nav bar, chip tròn) dùng CircleShape có sẵn.
val CinemaShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

// Cover art (ratio 2:3) dùng bán kính riêng 10dp — giữa small và medium,
// đủ mềm trên ảnh bìa mà không "nuốt" góc artwork
val CoverShape = RoundedCornerShape(10.dp)
