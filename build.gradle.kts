buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.google.com")
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath(kotlin("gradle-plugin:1.5.10"))
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
