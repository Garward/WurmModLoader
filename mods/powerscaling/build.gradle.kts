plugins {
    java
}

group = "com.garward.wurmmodloader.mods"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // WurmModLoader API - for event system
    implementation(project(":wurmmodloader-api"))

    // WurmModLoader core - for capability system
    implementation(project(":wurmmodloader-core"))

    // WurmModLoader modsupport - for hooks
    implementation(project(":wurmmodloader-modsupport"))

    // Depend on other mods for integration
    // Note: SoulboundGear integration will be added when we hook it in Phase 2
    // implementation(project(":mods:soulboundgear"))
    // implementation(project(":mods:materialsystem"))  // Future integration

    // UpgradeTree integration uses reflection to avoid circular dependency
    // (UpgradeTree depends on PowerScaling for power spending)

    // Wurm server dependencies (provided at runtime)
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))

    // bdew loot system integration
    compileOnly(files("../../distribution/bdew_server_mod_tools.jar"))

    // Testing
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.8.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("powerscaling")
    archiveVersion.set("") // Remove version number from JAR filename

    manifest {
        attributes(
            "Implementation-Title" to "Power Scaling Mod",
            "Implementation-Version" to project.version,
            "Built-By" to "WurmModLoader",
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
        )
    }
}

// Task to create distribution structure
tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("powerscaling")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/powerscaling")
    }

    from("src/dist") {
        into("mods/powerscaling")
    }

    from("README.md") {
        into("docs")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
