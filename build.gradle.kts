import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version Versions.KOTLIN apply false
    kotlin("plugin.serialization") version Versions.KOTLIN
    id("io.github.turansky.kfc.latest-webpack") version "5.50.0" apply false
}

allprojects {
    group = "net.perfectdreams.loritta"
    version = Versions.LORITTA

    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://oss.sonatype.org/content/repositories/snapshots/")

        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
        maven("https://repo.perfectdreams.net/")
        maven("https://jitpack.io")

        // Used by JDA
        maven("https://m2.dv8tion.net/releases")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

subprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = Versions.JVM_TARGET
        kotlinOptions.javaParameters = true
    }
}

// Gradle Build Scan
// https://stackoverflow.com/a/56634703/7271796
extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}