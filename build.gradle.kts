/*
 * 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
 * Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import gay.floof.gradle.utils.*
import java.text.SimpleDateFormat
import java.util.Date

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.floofy.dev/repo/releases")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.4")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
        classpath("gay.floof.utils:gradle-utils:1.3.0")
        classpath(kotlin("gradle-plugin", version = "1.6.21"))
        classpath(kotlin("serialization", version = "1.6.21"))
    }
}

plugins {
    kotlin("plugin.serialization") version "1.7.10"
    id("com.diffplug.spotless") version "6.11.0"
    kotlin("jvm") version "1.7.10"
    application
}

apply(plugin = "kotlinx-atomicfu")

val JAVA_VERSION = JavaVersion.VERSION_17
val VERSION = Version(4, 2, 0, 0, ReleaseType.None)

val commitHash by lazy {
    val cmd = "git rev-parse --short HEAD".split("\\s".toRegex())
    val proc = ProcessBuilder(cmd)
        .directory(File("."))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText().trim()
}

group = "gay.floof"
version = "$VERSION"

repositories {
    mavenCentral()
    mavenLocal()
    noel()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.perfectdreams.net/")
}

dependencies {
    // Kotlin libraries
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))

    // BOM
    api(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.4.1"))
    api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4"))
    api(platform("software.amazon.awssdk:bom:2.18.5"))
    api(platform("io.ktor:ktor-bom:2.1.3"))

    // kotlinx.coroutines libraries
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // kotlinx.serialization libraries
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core")

    // kotlinx.datetime libraries
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Noel Utilities
    floof("commons", "commons-slf4j", "1.3.0")
    floofy("ktor", "ktor-sentry", "0.0.1")

    // Apache Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Ktor Server libraries
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-default-headers")
    implementation("io.ktor:ktor-server-double-receive")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-serialization")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-cors")

    // Koin
    implementation("io.insert-koin:koin-core:3.2.2")

    // Redis (for caching api keys and ratelimits)
    implementation("io.lettuce:lettuce-core:6.2.1.RELEASE")

    // Logging with logback
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("ch.qos.logback:logback-core:1.4.4")
    api("org.slf4j:slf4j-api:2.0.3")

    // Conditional logic for logback
    implementation("org.codehaus.janino:janino:3.1.8")

    // Sentry
    implementation("io.sentry:sentry:6.6.0")
    implementation("io.sentry:sentry-logback:6.6.0")

    // Prometheus (for metrics)
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")
    implementation("io.prometheus:simpleclient_common:0.16.0")
    implementation("io.prometheus:simpleclient:0.16.0")

    // Discord Interactions
    implementation("net.perfectdreams.discordinteraktions:webserver-ktor-kord:0.0.15")

    // Redis (for ratelimiting cache)
    implementation("io.lettuce:lettuce-core:6.2.1.RELEASE")

    // PostgreSQL (for holding API keys)
    api(platform("org.jetbrains.exposed:exposed-bom:0.39.2"))
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime")
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // YAML (configuration)
    implementation("com.charleskorn.kaml:kaml:0.49.0")

    // OkHttp (for ktor client)
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-core")

    // Kord
    implementation("dev.kord:kord-core:0.8.0-M16")

    // JWT
    implementation("com.auth0:java-jwt:4.2.1")

    // S3
    implementation("software.amazon.awssdk:s3")
}

spotless {
    kotlin {
        trimTrailingWhitespace()
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        endWithNewline()

        // We can't use the .editorconfig file, so we'll have to specify it here
        // issue: https://github.com/diffplug/spotless/issues/142
        // ktlint 0.35.0 (default for Spotless) doesn't support trailing commas
        ktlint("0.43.0")
            .userData(
                mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                )
            )
    }
}

application {
    mainClass.set("gay.floof.hana.Bootstrap")
}

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        kotlinOptions.javaParameters = true
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    processResources {
        filesMatching("build-info.json") {
            val date = Date()
            val formatter = SimpleDateFormat("EEE, MMM d, YYYY - HH:mm:ss a")

            expand(
                mapOf(
                    "version" to rootProject.version,
                    "commit_sha" to commitHash,
                    "build_date" to formatter.format(date)
                )
            )
        }
    }
}
