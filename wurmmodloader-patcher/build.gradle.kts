plugins {
    id("java-library")
    id("application")
}

description = "WurmModLoader Bytecode Patcher"

application {
    mainClass.set("com.garward.wurmmodloader.patcher.PatchServerJar")
}

dependencies {
    // Core dependencies
    implementation(project(":wurmmodloader-core"))

    // Legacy compatibility layer (includes old listener interfaces for runtime)
    implementation(project(":wurmmodloader-legacy"))

    // Bytecode manipulation
    implementation("org.javassist:javassist:${project.property("javassistVersion")}")

    // Logging
    implementation("org.slf4j:slf4j-api:${project.property("slf4jVersion")}")
    implementation("ch.qos.logback:logback-classic:${project.property("logbackVersion")}")

    // Wurm Unlimited dependencies
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")
}

tasks.jar {
    // Ensure dependencies are built first
    dependsOn(configurations.runtimeClasspath)

    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader Patcher",
            "Implementation-Version" to project.version,
            "Main-Class" to "com.garward.wurmmodloader.patcher.PatchServerJar",
            "Automatic-Module-Name" to "com.garward.wurmmodloader.patcher"
        )
    }

    // Create fat JAR for patcher
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Create modlauncher.jar - fat JAR with correct launcher Main-Class
tasks.register<Jar>("modlauncherJar") {
    archiveBaseName.set("modlauncher")
    archiveVersion.set("")  // No version in filename

    // Ensure dependencies are built first
    dependsOn(configurations.runtimeClasspath)
    dependsOn(tasks.jar)

    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader",
            "Implementation-Version" to project.version,
            "Main-Class" to "com.garward.wurmmodloader.serverlauncher.ServerLauncher",
            "Class-Path" to "javassist.jar server.jar common.jar"
        )
    }

    // Include all classes from fat JAR (same as main jar task)
    from(zipTree(tasks.jar.get().archiveFile))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("build") {
    dependsOn("modlauncherJar")
}
