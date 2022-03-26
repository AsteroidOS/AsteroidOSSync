plugins {
    id("com.android.application")
}

android {
    compileSdk = 31
    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "org.asteroidos.sync"
        minSdk = 24
        targetSdk = 30
        versionCode = 22
        versionName = "0.22"
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
            }
            res {
                srcDir("src/main/lib/android-ripple-background/library/src/main/res/")
                srcDir("src/main/lib/material-intro-screen/material-intro-screen/src/main/res/")
                srcDir("src/main/lib/powerampapi/poweramp_api_lib/res/")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lint {
        checkReleaseBuilds = true
        disable += "MissingTranslation"
    }
}

repositories {
    mavenCentral()
    maven("https://maven.google.com")
    maven("https://jitpack.io")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.5.0")
    implementation("com.github.code-crusher:EasyWeather:1.2")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.osmdroid:osmdroid-android:6.1.6")
    implementation("no.nordicsemi.android.support.v18:scanner:1.5.1")
    implementation("no.nordicsemi.android:ble:2.3.1")
}
