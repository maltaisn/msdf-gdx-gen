import proguard.gradle.ProGuardTask
import java.util.Properties

plugins {
    kotlin("jvm")
    id("com.github.breadmoirai.github-release")
}

sourceSets {
    main {
        resources.srcDir("src/main/res")
    }
}

dependencies {
    val gdxVersion: String by project
    val coroutinesVersion: String by project
    val jcommanderVersion: String by project
    val pngtasticVersion: String by project

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("com.beust:jcommander:$jcommanderVersion")
    implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
    implementation("com.github.depsypher:pngtastic:$pngtasticVersion")
}

java {
    // Java 8 is needed by jcommander's more recent versions
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val mainClassName = "com.maltaisn.msdfgdx.gen.MainKt"
tasks.register<JavaExec>("run") {
    mainClass.set(mainClassName)
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    isIgnoreExitValue = true

    // Get program test arguments defined in local.properties.
    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())
    setArgsString(properties.getProperty("gen-test-args"))
    setWorkingDir(properties.getProperty("gen-test-working-dir"))

    if ("mac" in System.getProperty("os.name").toLowerCase()) {
        jvmArgs("-XstartOnFirstThread")
    }
}

// Use this task to create a fat jar.
val dist = tasks.register<Jar>("dist") {
    dependsOn("updateVersionRes")

    from(files(sourceSets.main.get().output.classesDirs))
    from(files(sourceSets.main.get().resources.srcDirs))
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("**/module-info.class")
    }
    archiveBaseName.set("msdfgen")

    manifest {
        attributes["Main-Class"] = mainClassName
    }
    finalizedBy(tasks.named("shrinkJar"))
}

tasks.register<ProGuardTask>("shrinkJar") {
    val distFile = dist.get().archiveFile.get().asFile
    configuration("proguard-rules.pro")
    // Use the jar task output as an input jar. This will automatically add the necessary task dependency.
    injars(distFile)

    val javaHome = System.getProperty("java.home")
    // Automatically handle the Java version of this build.
    if (System.getProperty("java.version").startsWith("1.")) {
        // Before Java 9, the runtime classes were packaged in a single jar file.
        libraryjars("$javaHome/lib/rt.jar")
    } else {
        // As of Java 9, the runtime classes are packaged in modular jmod files.
        libraryjars(
            mapOf("jarfilter" to "!**.jar",
                "filter" to "!module-info.class"),
            "$javaHome/jmods/java.base.jmod"
        )
    }
    outjars(file("$buildDir/msdfgen.jar"))

    libraryjars(configurations.runtimeClasspath.get().files)
}

tasks.register("updateVersionRes") {
    doLast {
        val genVersion: String by project
        val versionResFile = file("src/main/res/version.txt")
        versionResFile.writeText(genVersion)
    }
}

// Publish a new release to Github, using the lastest defined libVersion property,
// a git tag, and the release notes in CHANGELOG.md.
githubRelease {
    val genVersion: String by project

    if (project.hasProperty("githubReleasePluginToken")) {
        val githubReleasePluginToken: String by project
        token(githubReleasePluginToken)
    }
    owner("maltaisn")
    repo("msdf-gdx-gen")

    tagName("v$genVersion")
    targetCommitish("master")
    releaseName("v$genVersion")

    body {
        // Get release notes for version from changelog file.
        val changelog = file("../CHANGELOG.md")
        val versionChanges = StringBuilder()
        var foundVersion = false
        for (line in changelog.readLines()) {
            if (foundVersion && line.matches("""^#+\s*v.+$""".toRegex())) {
                break
            } else if (line.matches("""^#+\s*v$genVersion$""".toRegex())) {
                foundVersion = true
            } else if (foundVersion) {
                versionChanges.append(line)
                versionChanges.append('\n')
            }
        }
        if (!foundVersion) {
            throw GradleException("No release notes for version $genVersion")
        }
        versionChanges.toString().trim()
    }

    releaseAssets("$buildDir/msdfgen.jar")

    overwrite(true)
}
tasks.named("githubRelease") {
    dependsOn("build", "dist")
}
