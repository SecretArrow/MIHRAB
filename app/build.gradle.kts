import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Keystore produksi: dibaca dari keystore.properties (lokal, di-gitignore) atau env
// (CI: GitHub Secrets). JANGAN pernah commit keystore/token.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(propName)

android {
    namespace = "id.or.kemenag.yasintahlil"
    // compileSdk 36 bukan sekadar mengikuti versi terbaru: `targetSdk` tidak boleh melebihi
    // `compileSdk`, sedangkan Google Play mewajibkan API 36 untuk aplikasi baru sejak 31 Agustus
    // 2026. AGP 8.13 adalah rilis 8.x terakhir yang mengenal API 36 (batas atasnya API 36.1).
    compileSdk = 36

    defaultConfig {
        applicationId = "id.or.kemenag.yasintahlil"
        minSdk = 26
        targetSdk = 36
        // `versionCode` harus naik setiap rilis (Google Play menolak yang tidak naik), dan
        // `versionName` harus sama dengan nama tag — `release.yml` membandingkan keduanya dan
        // menggagalkan rilis bila berbeda, sehingga APK tak pernah mengaku versi lain dari tag-nya.
        versionCode = 15
        versionName = "1.10.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val path = secret("STORE_FILE", "storeFile")
            if (!path.isNullOrBlank() && file(path).exists()) {
                storeFile = file(path)
                storePassword = secret("STORE_PASSWORD", "storePassword")
                keyAlias = secret("KEY_ALIAS", "keyAlias")
                keyPassword = secret("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Hanya pakai signing produksi bila keystore tersedia; jika tidak, biarkan
            // unsigned agar CI tetap bisa memvalidasi build release/R8 tanpa rahasia.
            signingConfigs.getByName("release").storeFile?.let { signingConfig = signingConfigs.getByName("release") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// `kotlinOptions` sudah dihapus dari DSL Kotlin 2.3, jadi tingkat bytecode Java dinyatakan di sini.
// Nilainya harus sama dengan `compileOptions` di atas; `ReadmeToolchainTest` membandingkannya
// dengan "JDK 17" yang tertulis di README.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Peringatan kompilator = galat. Tanpa ini peringatan menumpuk tanpa ada yang membacanya:
        // setiap pemanggilan Gradle memakai `--build-cache`, dan tugas yang dilayani `FROM-CACHE`
        // tidak mencetak satu pun keluaran kompilator — sehingga log CI yang "bersih dari `w:`"
        // tidak membuktikan apa pun. Berlaku untuk SEMUA kompilasi modul ini (main, uji unit, uji
        // instrumentasi); `assembleDebugAndroidTest` di `ci.yml` karena itu ikut menjaga kelas
        // peringatan di `src/androidTest`, yang dulu baru terbaca setelah merge.
        allWarningsAsErrors.set(true)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)

    // Persistensi
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.workmanager)

    // Audio
    implementation(libs.media3.exoplayer)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // org.json asli untuk uji unit: android.jar hanya menyediakan stub yang melempar
    // "not mocked", sedangkan validator bundel konten memang harus diuji sungguhan.
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    // Tanpa `espresso`, `uiautomator`, dan `kotlinx.coroutines.test`: ketiganya dulu dideklarasikan
    // di sini padahal tidak satu pun kelasnya dipakai uji instrumentasi. `espresso-core` dan
    // `kotlinx-coroutines-test` tetap ada di classpath karena `compose-ui-test-junit4` menariknya
    // sendiri; `uiautomator` tidak dipakai apa pun. Deklarasi mati hanya memperbesar APK uji dan
    // memperlambat `e2e.yml`; `DependensiUjiTerpakaiTest` menjaganya agar tidak kembali tanpa dipakai.
    // Kerangka uji WorkManager: memeriksa pekerjaan yang benar-benar diantrekan penjadwal pengingat.
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
