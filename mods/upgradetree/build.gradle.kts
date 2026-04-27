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

    // WurmModLoader modsupport - for actions and hooks
    implementation(project(":wurmmodloader-modsupport"))

    // WurmModLoader core - for database utilities
    implementation(project(":wurmmodloader-core"))

    // PowerScaling integration - compile-time dependency for type safety
    compileOnly(project(":mods:powerscaling"))

    // declarativeui (CommunityMods) - compile-time access to Widgets/WidgetNode for
    // building the declarative UI tree. Pure factory API; declarativeui mod itself is
    // an optional runtime dep — the BML window stays the fallback when absent.
    compileOnly(files("${rootProject.projectDir}/lib/declarativeui.jar"))

    // Wurm server dependencies (provided at runtime)
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")

    // SQLite for database
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.8.9")

    // Testing
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.8.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("upgradetree")
    archiveVersion.set("") // Remove version number from JAR filename
    manifest {
        attributes(
            "Implementation-Title" to "Upgrade Tree Mod",
            "Implementation-Version" to project.version,
            "Built-By" to "WurmModLoader",
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
        )
    }
}

// Task to create distribution structure
tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("upgradetree")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/upgradetree")
    }

    from("src/dist") {
        into("mods/upgradetree")
    }

    from("DESIGN.md") {
        into("docs")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
