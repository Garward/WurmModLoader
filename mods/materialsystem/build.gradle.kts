plugins {
    java
}

group = "com.garward.wurmmodloader.mods"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // WurmModLoader API - for event system and registry
    implementation(project(":wurmmodloader-api"))

    // WurmModLoader core - for IconRegistry
    implementation(project(":wurmmodloader-core"))

    // WurmModLoader modsupport - for ItemTemplateBuilder
    implementation(project(":wurmmodloader-modsupport"))

    // Wurm server dependencies (provided at runtime)
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))

    // Testing
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.8.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("materialsystem")
    archiveVersion.set("")

    manifest {
        attributes(
            "Implementation-Title" to "Material System Mod",
            "Implementation-Version" to project.version,
            "Built-By" to "WurmModLoader",
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
        )
    }
}

// Task to create distribution structure
tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("materialsystem")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/materialsystem")
    }

    from("src/dist") {
        into("mods/materialsystem")
    }

    from("README.md") {
        into("docs")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
