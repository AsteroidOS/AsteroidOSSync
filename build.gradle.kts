buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.google.com")
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.2")
        classpath(kotlin("gradle-plugin:1.6.10"))
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
