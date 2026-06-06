import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    jacoco
}

android {
    namespace = "com.example.mybookslibrary"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "com.example.mybookslibrary"
        minSdk = 30
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.mybookslibrary.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // Robolectric cần merged manifest + resources (vd ComponentActivity của ui-test-manifest)
    // để chạy Compose UI test trên JVM.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    // MigrationTestHelper đọc schema JSON từ androidTest assets khi chạy trên emulator.
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

// Toolchain: ép compile/test bằng JDK 21 trên mọi máy, không phụ thuộc JAVA_HOME của contributor.
kotlin {
    jvmToolchain(21)
}

ksp {
    // Xuất schema Room để viết migration cho các version sau (đã bỏ destructive migration).
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Static analysis — baseline cho code cũ để không chặn CI, fix dần.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

ktlint {
    android = true
    // Bỏ qua code do KSP/Room/Hilt sinh ra (chỉ lint source do người viết).
    filter {
        exclude { it.file.path.contains("generated") }
        exclude { it.file.path.contains("build/") }
    }
}

// JaCoCo: báo cáo + ngưỡng coverage cho unit test JVM.
// Loại trừ code sinh tự động (Hilt/Room/Dagger/Navigation) và lớp test.
val jacocoGeneratedFilter =
    listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*_Hilt*.*",
        "**/Hilt_*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*Module_*.*",
        // Dùng *_Impl* (không phải *_Impl.*) để bắt cả nested class Room generated
        // (vd ChapterDao_Impl$1, AppDatabase_Impl$createOpenDelegate...): sau "_Impl" là "$"
        // chứ không phải ".", nên pattern cũ "**/*_Impl.*" bỏ sót → kéo coverage xuống giả tạo.
        "**/*_Impl*.*",
        // Kotlin sinh $DefaultImpls cho default method/param của interface (vd MangaDexApi).
        "**/*\$DefaultImpls.*",
        "**/Dagger*.*",
        "**/*Args.*",
        "**/*Directions.*",
        // Android/DI-glue không unit test được (chỉ wiring): Credential Manager,
        // Hilt NetworkModule (dựng OkHttp/Retrofit), Retrofit interface MangaDexApi (chỉ khai báo).
        "**/CredentialManagerGoogleSignInClient.*",
        "**/NetworkModule.*",
        "**/MangaDexApi.*",
        // Compose/Android reader glue is covered by focused UI tests where feasible. These file
        // facades mostly contain composable layout, pointer input, launchers, Toast/Intent, previews,
        // and Activity edge-to-edge wiring that are noisy or not meaningful in JVM line coverage.
        "**/MainActivity.*",
        "**/MainActivity\$*.*",
        "**/MainActivityKt.*",
        "**/AuthSession*.*",
        "**/ui/screens/reader/ReaderPreviewKt.*",
        "**/ui/screens/reader/ComposableSingletons\$ReaderPreviewKt.*",
        "**/ui/screens/reader/ReaderEffectHandlerKt*.*",
        "**/ui/screens/reader/HorizontalReaderTapObserverKt*.*",
        "**/ui/screens/reader/HorizontalReaderContentKt*.*",
        "**/ui/screens/reader/ComposableSingletons\$HorizontalReaderContentKt.*",
        "**/ui/screens/reader/VerticalReaderContentKt*.*",
        "**/ui/screens/reader/MangaPageItemKt*.*",
        "**/ui/screens/reader/ComposableSingletons\$MangaPageItemKt.*",
        "**/ui/screens/reader/WebtoonPageItemKt*.*",
        "**/ui/screens/reader/ComposableSingletons\$WebtoonPageItemKt.*",
        "**/ui/screens/reader/ReaderBarsKt*.*",
        "**/ui/screens/reader/ComposableSingletons\$ReaderBarsKt.*",
        "**/ui/screens/reader/components/PageActionBottomSheetKt*.*",
        "**/ui/screens/reader/components/ComposableSingletons\$PageActionBottomSheetKt.*",
        // detail/MangaDetailScreen là delegate thuần — chỉ gọi thẳng ui.screens.MangaDetailScreen,
        // không có logic riêng. Test ở ui.screens đã cover.
        "**/ui/screens/detail/MangaDetailScreen*.*",
        // MyBooksLibraryApp là @HiltAndroidApp — wiring WorkManager + crash handler + Timber.
        // Không test được với JVM unit test (cần Application context đầy đủ).
        "**/MyBooksLibraryApp*.*",
        "**/ImageLoaderEntryPoint*.*",
        // UiText.asString() là @Composable — không chạy được trên JVM unit test.
        "**/ui/util/UiText*.*",
    )

// AGP 9 "built-in Kotlin" xuất class compile ở built_in_kotlinc (không phải tmp/kotlin-classes).
fun jacocoClassTree() =
    fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoGeneratedFilter)
    }

// Trỏ đúng file .exec (không quét cả build/) để tránh implicit-dependency với task ktlint.
fun jacocoExec() = files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))

// Gộp .exec (unit test) + .ec (instrumented/emulator test) để đo coverage thật sau khi
// cả hai loại test đã chạy. Dùng bởi CI job emulator-test sau khi connected test xong.
fun jacocoExecCombined() = fileTree(layout.buildDirectory.get()) {
    include(
        "jacoco/testDebugUnitTest.exec", // unit test (JVM)
        "outputs/code_coverage/**/*.ec", // instrumented test (emulator, AGP path)
        "**/*.ec", // fallback nếu AGP thay đổi path
    )
    exclude("tmp/**", "generated/**")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(jacocoClassTree())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(jacocoExec())
}

// Report gộp unit test + instrumented test: dùng sau khi cả hai đã chạy trên CI.
// Không dependsOn task test để CI tự quyết định thứ tự chạy.
tasks.register<JacocoReport>("jacocoCombinedReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(jacocoClassTree())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(jacocoExecCombined())
}

// Ngưỡng coverage RATCHET: sàn = mức hiện tại (~10%), chỉ chặn khi coverage TỤT. Siết dần đợt sau.
tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(jacocoClassTree())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(jacocoExec())
    violationRules {
        // Sàn ratchet toàn project — siết dần theo mức thật:
        //   0.20 (2026-06-05, data) → 0.30 (sau ui.viewmodel) → 0.70 (sau ui.screens + reader).
        // ~3pp headroom dưới 73.3% thật; UI nav/emulator chưa test → chưa nâng cao hơn.
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
        // Lớp logic (data.repository + domain + ui.viewmodel) đã phủ kỹ → giữ ngưỡng LINE cao.
        // ui.viewmodel chỉ gate LINE (branch ~71% do nhánh defensive/effect khó ép).
        rule {
            element = "PACKAGE"
            includes =
                listOf(
                    "com.example.mybookslibrary.data.repository",
                    "com.example.mybookslibrary.domain.usecase",
                    "com.example.mybookslibrary.domain.model",
                    "com.example.mybookslibrary.ui.viewmodel",
                )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
        // Tầng data còn lại (local/dao/remote/models/download) đã phủ test JVM kỹ:
        // LINE ≥90% + BRANCH ≥80% (branch luôn thấp hơn line do nhánh platform/IO-error
        // khó ép trong unit test — không chặn cao hơn để tránh fragile gate).
        rule {
            element = "PACKAGE"
            includes =
                listOf(
                    "com.example.mybookslibrary.data.local",
                    "com.example.mybookslibrary.data.local.dao",
                    "com.example.mybookslibrary.data.remote",
                    "com.example.mybookslibrary.data.remote.models",
                    "com.example.mybookslibrary.data.download",
                )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoCoverageVerification")
}

// JaCoCo cần includeNoLocationClasses để đo coverage code chạy qua Robolectric (sandbox classloader);
// thiếu cờ này, class test bằng Robolectric bị báo 0% dù đã test.
tasks.withType<Test>().configureEach {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

dependencies {
    // Shared dependencies are added to each required source set from one declaration
    // so IDE inspections do not report them as declared multiple times.
    val composeBom = platform(libs.androidx.compose.bom)
    listOf("implementation", "androidTestImplementation").forEach { add(it, composeBom) }
    listOf("ksp", "kspTest", "kspAndroidTest").forEach { add(it, libs.hilt.android.compiler) }
    listOf("testImplementation", "androidTestImplementation").forEach { add(it, libs.hilt.android.testing) }
    listOf("testImplementation", "androidTestImplementation").forEach { add(it, libs.androidx.compose.ui.test.junit4) }
    listOf("testImplementation", "debugImplementation").forEach { add(it, libs.androidx.compose.ui.test.manifest) }

    // Android core and lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose UI and navigation
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.telephoto.zoomable.image.coil3)

    // Local data and background work
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    // Network and image loading
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.timber)

    // Dependency injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Authentication
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // JVM tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotest.property)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Cài git hooks từ .githooks/ khi build lần đầu sau khi clone.
// `git config core.hooksPath` chạy <10ms và idempotent — không ảnh hưởng build time đáng kể.
tasks.register<Exec>("installGitHooks") {
    group = "setup"
    description = "Trỏ git hooks về .githooks/ để pre-commit hook active sau khi clone"
    commandLine("git", "config", "core.hooksPath", ".githooks")
    isIgnoreExitValue = true
}

tasks.named("preBuild").configure {
    dependsOn("installGitHooks")
}
