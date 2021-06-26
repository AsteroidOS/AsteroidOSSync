buildscript {
    repositories {
        jcenter()
        maven("https://maven.google.com")
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath(kotlin("gradle-plugin:1.5.10"))
    }
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}
