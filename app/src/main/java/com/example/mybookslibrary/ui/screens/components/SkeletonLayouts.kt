package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mybookslibrary.ui.theme.CoverShape
import com.example.mybookslibrary.ui.theme.Dimens

@Composable
fun DiscoverSkeletonLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = Dimens.SpacingLg),
    ) {
        SkeletonShimmer(
            Modifier
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .fillMaxWidth()
                .height(Dimens.SpacingXxl)
                .shimmerShape(MaterialTheme.shapes.medium),
        )
        Spacer(Modifier.height(Dimens.SpacingLg))

        SkeletonShimmer(
            Modifier
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shimmerShape(MaterialTheme.shapes.large),
        )
        Spacer(Modifier.height(Dimens.SpacingXxl))

        SkeletonShimmer(
            Modifier
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .width(Dimens.CoverCard)
                .height(Dimens.SpacingLg)
                .shimmerShape(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(Dimens.SpacingMd))

        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingCompact),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
            userScrollEnabled = false,
        ) {
            items(SKELETON_COVER_COUNT) {
                Column(modifier = Modifier.width(Dimens.CoverCard)) {
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .shimmerShape(CoverShape),
                    )
                    Spacer(Modifier.height(Dimens.SpacingSm))
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .height(Dimens.SpacingMd)
                            .shimmerShape(MaterialTheme.shapes.small),
                    )
                }
            }
        }
        Spacer(Modifier.height(Dimens.SpacingXxl))

        SkeletonShimmer(
            Modifier
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .width(Dimens.CoverCard)
                .height(Dimens.SpacingLg)
                .shimmerShape(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(Dimens.SpacingMd))

        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingCompact),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
            userScrollEnabled = false,
        ) {
            items(SKELETON_COVER_COUNT) {
                Column(modifier = Modifier.width(Dimens.CoverCard)) {
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .shimmerShape(CoverShape),
                    )
                    Spacer(Modifier.height(Dimens.SpacingSm))
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .height(Dimens.SpacingMd)
                            .shimmerShape(MaterialTheme.shapes.small),
                    )
                }
            }
        }
    }
}

@Composable
fun DetailSkeletonLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Dimens.ScreenPaddingCompact)) {
        SkeletonShimmer(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .shimmerShape(MaterialTheme.shapes.large),
        )
        Spacer(Modifier.height(Dimens.SpacingXl))

        SkeletonShimmer(
            Modifier
                .fillMaxWidth(0.7f)
                .height(Dimens.SpacingXl)
                .shimmerShape(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(Dimens.SpacingMd))

        SkeletonShimmer(
            Modifier
                .fillMaxWidth(0.4f)
                .height(Dimens.SpacingLg)
                .shimmerShape(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(Dimens.SpacingXxl))

        repeat(SKELETON_CHAPTER_COUNT) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpacingSm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
            ) {
                SkeletonShimmer(
                    Modifier
                        .weight(1f)
                        .height(Dimens.SpacingLg)
                        .shimmerShape(MaterialTheme.shapes.small),
                )
                SkeletonShimmer(
                    Modifier
                        .width(Dimens.IconXl)
                        .height(Dimens.SpacingLg)
                        .shimmerShape(MaterialTheme.shapes.small),
                )
            }
        }
    }
}

@Composable
fun SearchSkeletonLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Dimens.ScreenPaddingCompact)) {
        Spacer(Modifier.height(Dimens.SpacingLg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        ) {
            repeat(SKELETON_GRID_COLS) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .shimmerShape(CoverShape),
                    )
                    Spacer(Modifier.height(Dimens.SpacingSm))
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .height(Dimens.SpacingMd)
                            .shimmerShape(MaterialTheme.shapes.small),
                    )
                }
            }
        }
        Spacer(Modifier.height(Dimens.SpacingLg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        ) {
            repeat(SKELETON_GRID_COLS) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .shimmerShape(CoverShape),
                    )
                    Spacer(Modifier.height(Dimens.SpacingSm))
                    SkeletonShimmer(
                        Modifier
                            .fillMaxWidth()
                            .height(Dimens.SpacingMd)
                            .shimmerShape(MaterialTheme.shapes.small),
                    )
                }
            }
        }
    }
}

private fun Modifier.shimmerShape(shape: androidx.compose.ui.graphics.Shape) =
    this.clip(shape)

private const val SKELETON_COVER_COUNT = 4
private const val SKELETON_CHAPTER_COUNT = 6
private const val SKELETON_GRID_COLS = 3
