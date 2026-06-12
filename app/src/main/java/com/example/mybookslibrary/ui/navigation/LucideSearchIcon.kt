package com.example.mybookslibrary.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

// File riêng: ở đây `Search` resolve đúng sang Lucide extension property
// (không xung đột với nav `Search` destination ở MainNavGraph.kt).
val LucideSearchIcon: ImageVector = Lucide.Search
