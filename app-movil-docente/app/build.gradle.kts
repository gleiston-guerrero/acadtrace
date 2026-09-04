import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    jacoco
}

val localSigningProperties = Properties()

val propertiesFile = rootProject.file("keystore.properties")
if (propertiesFile.isFile) {
    propertiesFile.inputStream().use { input ->
        localSigningProperties.load(input)
    }
}
fun signingValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull ?: localSigningProperties.getProperty(propertyName)

val releaseStoreFile = signingValue("SGA_RELEASE_STORE_FILE", "storeFile")
val releaseStorePassword = signingValue("SGA_RELEASE_STORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("SGA_RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("SGA_RELEASE_KEY_PASSWORD", "keyPassword")
val releaseSigningReady = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() } && releaseStoreFile?.let { file(it).isFile } == true

android {
    namespace = "ec.edu.uteq.sga.representante"
    compileSdk = 34

    defaultConfig {
        applicationId = "ec.edu.uteq.sga.representante"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

jacoco { toolVersion = "0.8.13" }

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        html.required.set(true)
        html.outputLocation.set(rootProject.layout.projectDirectory.dir("../docs/cobertura/movil"))
        xml.required.set(true)
        xml.outputLocation.set(rootProject.layout.projectDirectory.file("../docs/cobertura/movil/jacoco.xml"))
        csv.required.set(false)
    }
    val generated = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*_Impl*.*")
    classDirectories.setFrom(files(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(generated) },
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) { exclude(generated) }
    ))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.exec") })
}

gradle.taskGraph.whenReady {
    val requestsReleaseApk = allTasks.any { it.path == ":app:assembleRelease" }
    if (requestsReleaseApk && !releaseSigningReady) {
        throw GradleException(
            "Firma release no configurada. Define las variables SGA_RELEASE_* o crea keystore.properties local ignorado."
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit & Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Security EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
