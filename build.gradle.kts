buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.google.com")
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.0-alpha06")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
