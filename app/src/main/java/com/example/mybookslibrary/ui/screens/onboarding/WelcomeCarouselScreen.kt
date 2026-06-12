@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.screens.components.AppButton
import com.example.mybookslibrary.ui.screens.components.AppButtonStyle
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.LocalReducedMotion
import com.example.mybookslibrary.ui.util.appString
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector,
)

private val pages =
    listOf(
        OnboardingPage(R.string.onboarding_page1_title, R.string.onboarding_page1_body, Lucide.Compass),
        OnboardingPage(R.string.onboarding_page2_title, R.string.onboarding_page2_body, Lucide.BookOpen),
        OnboardingPage(R.string.onboarding_page3_title, R.string.onboarding_page3_body, Lucide.Smartphone),
    )

@Suppress("LongMethod")
@Composable
fun WelcomeCarouselScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val page = pages[pageIndex]
            val pageOffset =
                (pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction)
                    .absoluteValue.coerceIn(0f, 1f)
            val parallaxTranslation = if (reducedMotion) 0f else pageOffset * 80f

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.SpacingXxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(Dimens.IconHero)
                            .graphicsLayer { translationY = -parallaxTranslation },
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Dimens.SpacingXxl))
                Text(
                    appString(page.titleRes),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationY = parallaxTranslation * 0.5f },
                )
                Spacer(Modifier.height(Dimens.SpacingLg))
                Text(
                    appString(page.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = Dimens.SpacingXl, vertical = Dimens.SpacingXl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                repeat(pages.size) { index ->
                    val isActive = index == pagerState.currentPage
                    val dotAlpha by animateFloatAsState(
                        if (isActive) 1f else 0.3f,
                        label = "dotAlpha",
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(if (isActive) 10.dp else 8.dp)
                                .alpha(dotAlpha)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(Dimens.SpacingXl))
            AppButton(
                text =
                    if (isLastPage) {
                        appString(R.string.onboarding_get_started)
                    } else {
                        appString(R.string.onboarding_next)
                    },
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.SpacingSm))
            if (!isLastPage) {
                AppButton(
                    text = appString(R.string.onboarding_skip),
                    onClick = onFinish,
                    style = AppButtonStyle.Text,
                )
            }
        }
    }
}
