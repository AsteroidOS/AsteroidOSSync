plugins {
	id("com.android.application")
	kotlin("android")
}

android {
	compileSdkVersion(30)
	buildToolsVersion = "30.0.2"

	defaultConfig {
		applicationId = "org.asteroidos.sync"
		minSdkVersion(21)
		targetSdkVersion(30)
		versionCode = 19
		versionName = "0.19"
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
				srcDir("src/main/lib/sweetblue/library/src/main/java/")
				srcDir("src/main/lib/android-ripple-background/library/src/main/java/")
				srcDir("src/main/lib/material-intro-screen/material-intro-screen/src/main/java/")
				srcDir("src/main/lib/powerampapi/poweramp_api_lib/src/")
			}
			res {
				srcDir("src/main/lib/sweetblue/library/src/main/res/")
				srcDir("src/main/lib/android-ripple-background/library/src/main/res/")
				srcDir("src/main/lib/material-intro-screen/material-intro-screen/src/main/res/")
				srcDir("src/main/lib/powerampapi/poweramp_api_lib/res/")
			}
		}
	}

	lintOptions {
		isCheckReleaseBuilds = false
	}
}

repositories {
	mavenCentral()
	maven("https://maven.google.com")
}

dependencies {
	implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
	testImplementation("junit:junit:4.13.1")
	implementation("androidx.appcompat:appcompat:1.2.0")
	implementation("androidx.legacy:legacy-support-v4:1.0.0")
	implementation("androidx.cardview:cardview:1.0.0")
	implementation("androidx.recyclerview:recyclerview:1.1.0")
	implementation("com.google.android.material:material:1.2.1")
	implementation("github.vatsal.easyweather:library:1.0.0")
	implementation("com.google.code.gson:gson:2.8.6")
	implementation("org.osmdroid:osmdroid-android:6.1.6")
}
