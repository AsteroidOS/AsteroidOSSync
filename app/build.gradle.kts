plugins {
    alias(libs.plugins.android.application)
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "org.asteroidos.sync"
        minSdk = 24
        targetSdk = 34
        versionCode = 29
        versionName = "0.29"
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    sourceSets {
        named("main") {
            java {
                srcDir("src/main/lib/android-ripple-background/library/src/main/java/")
                srcDir("src/main/lib/material-intro-screen/material-intro-screen/src/main/java/")
                srcDir("src/main/lib/powerampapi/poweramp_api_lib/src/")
                srcDir("src/main/lib/easyweather/src/main/java/")
            }
            res {
                srcDir("src/main/lib/android-ripple-background/library/src/main/res/")
                srcDir("src/main/lib/material-intro-screen/material-intro-screen/src/main/res/")
                srcDir("src/main/lib/powerampapi/poweramp_api_lib/res/")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        checkReleaseBuilds = true
        disable += "MissingTranslation"
    }
    namespace = "org.asteroidos.sync"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    testImplementation(libs.junit)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.material)
    // EasyWeather is vendored under src/main/lib/easyweather (see its README);
    // these are the runtime dependencies it needs, from Maven Central.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.osmdroid.android)
    implementation(libs.nordic.scanner)
    implementation(libs.nordic.ble)
}
