plugins {
    java
}

group = "com.garward.wurmmodloader.examples"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // WurmModLoader API - for event system
    implementation(project(":wurmmodloader-api"))

    // WurmModLoader modsupport - for ItemTemplateBuilder
    implementation(project(":wurmmodloader-modsupport"))

    // Wurm server dependencies (provided at runtime)
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("oversizedclub")

    manifest {
        attributes(
            "Implementation-Title" to "Oversized Club Mod",
            "Implementation-Version" to project.version,
            "Built-By" to "WurmModLoader",
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
        )
    }
}

// Task to create distribution structure
tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("oversizedclub")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/oversizedclub")
    }

    from("src/dist") {
        into("mods")
    }

    from("README.md") {
        into("docs")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
