buildscript {
    val kotlinVersion: String by project
    val proguardLocation: String by project
    val githubReleasePluginVersion: String by project
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        flatDir("dirs" to proguardLocation)
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath(":proguard:")
        classpath("com.github.breadmoirai:github-release:$githubReleasePluginVersion")
    }
}

plugins {
    base
}

allprojects {
    repositories {
        jcenter()
        google()
        mavenCentral()
    }
}

tasks.clean {
    delete(project.buildDir)
}
