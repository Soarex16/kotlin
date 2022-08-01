plugins {
    kotlin("js")
    `maven-publish`
}

group = "kotlinx.webworkers"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    testImplementation(kotlin("test"))
}

kotlin {
    js(IR) {
        browser()
        nodejs()
        binaries.library()
    }
}

configureCommonPublicationSettingsForGradle(signingRequired = false)

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
