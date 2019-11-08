buildscript {
    val kotlinVersion: String by project
    val proguardLocation: String by project
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        flatDir("dirs" to proguardLocation)
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath(":proguard:")
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

tasks.named("clean") {
    delete(project.buildDir)
}
