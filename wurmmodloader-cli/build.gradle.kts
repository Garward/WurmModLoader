plugins {
    id("java-library")
    id("application")
}

description = "WurmModLoader Command-Line Tools"

application {
    mainClass.set("com.garward.wurmmodloader.cli.WurmModLoaderCli")
}

dependencies {
    // Core dependencies
    implementation(project(":wurmmodloader-api"))
    implementation(project(":wurmmodloader-core"))

    // CLI framework
    implementation("info.picocli:picocli:${project.property("picocliVersion")}")
    annotationProcessor("info.picocli:picocli-codegen:${project.property("picocliVersion")}")

    // JSON parsing for mod descriptors
    implementation("com.fasterxml.jackson.core:jackson-databind:${project.property("jacksonVersion")}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:${project.property("jacksonVersion")}")

    // Logging
    implementation("org.slf4j:slf4j-api:${project.property("slf4jVersion")}")
    implementation("ch.qos.logback:logback-classic:${project.property("logbackVersion")}")
}

tasks.jar {
    // Ensure dependencies are built first
    dependsOn(configurations.runtimeClasspath)

    manifest {
        attributes(
            "Implementation-Title" to "WurmModLoader CLI",
            "Implementation-Version" to project.version,
            "Main-Class" to "com.garward.wurmmodloader.cli.WurmModLoaderCli",
            "Automatic-Module-Name" to "com.garward.wurmmodloader.cli"
        )
    }

    // Create fat JAR for CLI
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Task to create native executable (optional, requires GraalVM)
tasks.register("nativeCompile") {
    dependsOn("jar")
    group = "build"
    description = "Compile to native executable using GraalVM native-image"

    doLast {
        if (System.getenv("GRAALVM_HOME") != null) {
            exec {
                commandLine(
                    "${System.getenv("GRAALVM_HOME")}/bin/native-image",
                    "-jar", "${buildDir}/libs/${project.name}-${project.version}.jar",
                    "-H:Name=wurmmodloader",
                    "--no-fallback",
                    "--enable-preview"
                )
            }
        } else {
            println("GRAALVM_HOME not set. Skipping native compilation.")
        }
    }
}
