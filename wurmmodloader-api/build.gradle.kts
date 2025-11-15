plugins {
    id("java-library")
}

description = "WurmModLoader Public API"

dependencies {
    // Wurm Unlimited dependencies (compile-only for interfaces)
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader API",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "com.garward.wurmmodloader.api"
        )
    }
}
