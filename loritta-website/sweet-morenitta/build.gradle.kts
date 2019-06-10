import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val loriVersion by lazy { ext["lori-version"] as String }
val kotlinVersion by lazy { ext["kotlin-version"] as String }
val ktorVersion by lazy { ext["ktor-version"] as String }

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

plugins {
    java
    kotlin("jvm")
    `maven-publish`
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}

repositories {
    jcenter()
}

dependencies {
    compile(project(":loritta-api-jvm"))
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    compile("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-script-util:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")
    compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")
    compile("com.github.ben-manes.caffeine:caffeine:2.7.0")
    compile("org.postgresql:postgresql:42.2.5")
    compile("com.zaxxer:HikariCP:3.3.1")
    compile("org.jetbrains.exposed:exposed:0.13.6")
    compile("io.ktor:ktor-server-core:$ktorVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-websockets:$ktorVersion")
    compile("io.ktor:ktor-client-core:$ktorVersion")
    compile("io.ktor:ktor-client-cio:$ktorVersion")
    compile("org.jooby:jooby-mongodb:1.6.0")
    compile("io.github.microutils:kotlin-logging:1.6.26")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.8")
    compile("com.fasterxml.jackson.core:jackson-annotations:2.9.8")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.8")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
    compile("com.fasterxml.jackson.module:jackson-module-parameter-names:2.9.8")
    compile("org.honton.chas.hocon:jackson-dataformat-hocon:1.1.1")
    compile("com.github.salomonbrys.kotson:kotson:2.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.0-M1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.0-M1")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.assertj:assertj-core:3.12.2")
}


tasks.test {
    useJUnitPlatform()
}