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

    // WurmModLoader modsupport - for actions and hooks
    implementation(project(":wurmmodloader-modsupport"))

    // WurmModLoader core - for database utilities
    implementation(project(":wurmmodloader-core"))

    // WurmModLoader legacy - for org.gotti.wurmunlimited.modsupport.actions.*
    implementation(project(":wurmmodloader-legacy"))

    // Material System mod - for material bonus queries
    implementation(project(":mods:materialsystem"))

    // Wurm server dependencies (provided at runtime)
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))

    // SQLite for database
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    // Testing
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.8.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("soulboundgear")
    archiveVersion.set("")

    manifest {
        attributes(
            "Implementation-Title" to "Soulbound Gear Mod",
            "Implementation-Version" to project.version,
            "Built-By" to "WurmModLoader",
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
        )
    }
}

// Task to create distribution structure
tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("soulboundgear")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/soulboundgear")
    }

    from("src/dist") {
        into("mods/soulboundgear")
    }

    from("README.md") {
        into("docs")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
