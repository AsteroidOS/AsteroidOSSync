buildscript {
    repositories {
        jcenter()
        maven("https://maven.google.com")
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
    }
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}
