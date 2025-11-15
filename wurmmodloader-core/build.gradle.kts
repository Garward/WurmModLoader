plugins {
    id("java-library")
}

description = "WurmModLoader Core Implementation"

dependencies {
    // API dependency (includes legacy interfaces for backward compatibility)
    api(project(":wurmmodloader-api"))

    // Bytecode manipulation
    implementation("org.javassist:javassist:${project.property("javassistVersion")}")
    implementation("com.google.code.gson:gson:2.10.1")

    // YAML configuration parsing
    implementation("org.yaml:snakeyaml:2.2")

    // Logging
    implementation("org.slf4j:slf4j-api:${project.property("slf4jVersion")}")
    implementation("ch.qos.logback:logback-classic:${project.property("logbackVersion")}")

    // Wurm Unlimited dependencies (compile-only)
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")

    // ServerPacks API (compile-only for icon pack registration)
    compileOnly(files("/home/garward/.local/share/Steam/steamapps/common/Wurm Unlimited Dedicated Server/mods/serverpacks/serverpacks.jar"))

    // Test dependencies for Wurm classes
    testImplementation("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    testImplementation("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader Core",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "com.garward.wurmmodloader.core",
            "Add-Opens" to "java.base/java.lang java.base/java.util"
        )
    }
}
