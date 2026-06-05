package com.example.mybookslibrary.ui.navigation

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity rỗng dùng làm host cho Hilt + Robolectric test.
 * Cần @AndroidEntryPoint để hiltViewModel() bên trong Composable hoạt động đúng.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
