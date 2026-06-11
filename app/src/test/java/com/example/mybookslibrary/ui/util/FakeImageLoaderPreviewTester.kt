package com.example.mybookslibrary.ui.util

import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi

/**
 * Tester cho preview-gen screenshot tests: cài [FakeImageLoader] trước mỗi capture
 * để Coil trả ColorDrawable đồng bộ thay vì fetch network.
 *
 * Không có nó, preview chứa AsyncImage render nondeterministic (lúc capture được
 * trang đen "đang load", lúc thì "Failed to load image") → golden flaky giữa
 * record và verify dù UI không đổi.
 *
 * Được tham chiếu qua `testerQualifiedClassName` trong roborazzi DSL (build.gradle.kts).
 */
@OptIn(ExperimentalRoborazziApi::class)
@coil3.annotation.ExperimentalCoilApi
class FakeImageLoaderPreviewTester private constructor(
    private val delegate: AndroidComposePreviewTester,
) : ComposePreviewTester<AndroidPreviewJUnit4TestParameter> by delegate {
    constructor() : this(AndroidComposePreviewTester())

    override fun test(testParameter: AndroidPreviewJUnit4TestParameter) {
        FakeImageLoader.install()
        delegate.test(testParameter)
    }
}
