plugins {
    id("java-library")
}

description = "WurmModLoader Legacy Compatibility Layer"

dependencies {
    // Core and API dependencies
    api(project(":wurmmodloader-api"))
    implementation(project(":wurmmodloader-core"))

    // Bytecode manipulation
    implementation("org.javassist:javassist:${project.property("javassistVersion")}")

    // Wurm Unlimited dependencies
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")

}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader Legacy Bridge",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "com.garward.wurmmodloader.legacy"
        )
    }
}
