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
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        "**/*_Impl.*",
        "**/Dagger*.*",
        "**/*Args.*",
        "**/*Directions.*",
        // Android-glue (Credential Manager) — không unit test được, đã tách khỏi AuthRepository.
        "**/CredentialManagerGoogleSignInClient.*",
    )

// AGP 9 "built-in Kotlin" xuất class compile ở built_in_kotlinc (không phải tmp/kotlin-classes).
fun jacocoClassTree() =
    fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoGeneratedFilter)
    }

// Trỏ đúng file .exec (không quét cả build/) để tránh implicit-dependency với task ktlint.
fun jacocoExec() = files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))

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

// Ngưỡng coverage RATCHET: sàn = mức hiện tại (~10%), chỉ chặn khi coverage TỤT. Siết dần đợt sau.
tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(jacocoClassTree())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(jacocoExec())
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.20".toBigDecimal()
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.telephoto.zoomable.image.coil3)
    implementation(libs.timber)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
