plugins {
    id("com.android.application")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("30.0.2")

    defaultConfig {
        applicationId("org.asteroidos.sync")
        minSdkVersion(24)
        targetSdkVersion(30)
        versionCode(21)
        versionName("0.21")
    }
    buildTypes {
        named("release") {
            minifyEnabled(false)
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

    lintOptions {
        isCheckReleaseBuilds = false
        disable("MissingTranslation")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

repositories {
    mavenCentral()
    maven("https://maven.google.com")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("github.vatsal.easyweather:library:1.0.0")
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("org.osmdroid:osmdroid-android:6.1.6")
    implementation("no.nordicsemi.android.support.v18:scanner:1.5.0")
    implementation("no.nordicsemi.android:ble:2.2.4")
}
