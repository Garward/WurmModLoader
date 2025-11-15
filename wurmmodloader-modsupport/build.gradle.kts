plugins {
    id("java-library")
}

description = "WurmModLoader Mod Support Utilities"

dependencies {
    // Core dependencies
    api(project(":wurmmodloader-api"))
    implementation(project(":wurmmodloader-core"))

    // Bytecode manipulation
    implementation("org.javassist:javassist:${project.property("javassistVersion")}")

    // Wurm Unlimited dependencies
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")

    // Test dependencies
    testImplementation("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    testImplementation("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader Mod Support",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "com.garward.wurmmodloader.modsupport"
        )
    }
}
