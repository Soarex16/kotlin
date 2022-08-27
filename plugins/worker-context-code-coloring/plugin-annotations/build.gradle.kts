import plugins.signLibraryPublication

plugins {
    kotlin("js")
    `maven-publish`
}

group = "org.jetbrains.kotlin"

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir("src")
    }
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        js(IR).compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

configureCommonPublicationSettingsForGradle(signLibraryPublication)

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}


