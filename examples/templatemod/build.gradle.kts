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

    // WurmModLoader core - for event subscriptions
    implementation(project(":wurmmodloader-core"))

    // WurmModLoader modsupport - for BML builder and questions
    implementation(project(":wurmmodloader-modsupport"))

    // Wurm server dependencies (provided at runtime)
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("templatemod")
    archiveVersion.set("")  // No version in JAR name

    manifest {
        attributes(
            "Implementation-Title" to "Template Mod Example",
            "Implementation-Version" to project.version,
            "Built-By" to "WurmModLoader"
        )
    }
}

// Task to create distribution structure
tasks.register<Copy>("modDistribution") {
    dependsOn(tasks.jar)

    destinationDir = file("${buildDir}/distribution")

    // Copy JAR to mods/templatemod/
    from(tasks.jar) {
        into("mods/templatemod")
    }

    // Copy properties to mods/
    from("src/dist") {
        into("mods")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
