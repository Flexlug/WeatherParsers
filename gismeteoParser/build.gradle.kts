plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.8.20"
    application
}

group = "org.flexlug"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    testImplementation(kotlin("test"))

    implementation("org.jsoup:jsoup:1.15.4")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.0") // for JVM platform
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}