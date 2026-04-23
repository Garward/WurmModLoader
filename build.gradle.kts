plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "com.garward.wurmmodloader"
    version = "0.9.1"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://gotti.no-ip.org/maven/repository")
            name = "WurmUnlimited"
        }
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        // Target Java 8 bytecode for compatibility with Wurm server
        // But use Java 17 toolchain for building (configured above)
        options.release.set(8)
    }

    dependencies {
        // Common test dependencies - JUnit 4 for legacy tests
        testImplementation("junit:junit:4.13.2")
        testImplementation("org.assertj:assertj-core:3.24.2")

        // Also include JUnit 5 for future tests
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).apply {
                addBooleanOption("html5", true)
                addStringOption("Xdoclint:none", "-quiet")
                encoding = "UTF-8"
                docEncoding = "UTF-8"
                charSet = "UTF-8"

                links(
                    "https://docs.oracle.com/en/java/javase/17/docs/api/"
                )
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set("WurmModLoader ${project.name}")
                    description.set("Modern modding framework for Wurm Unlimited")
                    url.set("https://github.com/garward/WurmModLoader")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("garward")
                            name.set("Garward")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/garward/WurmModLoader.git")
                        developerConnection.set("scm:git:ssh://github.com:garward/WurmModLoader.git")
                        url.set("https://github.com/garward/WurmModLoader")
                    }
                }
            }
        }
    }
}

// Root project tasks
tasks.register("cleanAll") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
    description = "Clean all subprojects"
    group = "build"
}

tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
    description = "Build all subprojects"
    group = "build"
}

tasks.register("testAll") {
    dependsOn(subprojects.map { it.tasks.named("test") })
    description = "Run tests in all subprojects"
    group = "verification"
}

// =============================
// Production Runtime ZIP
// =============================
tasks.register<Zip>("dist") {
    group = "distribution"
    description = "Creates complete runtime ZIP with all core modules"

    dependsOn("buildAll")

    archiveBaseName.set("WurmModloader-Runtime")
    archiveVersion.set(version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    // 🩹 Fix duplicate JARs
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val modules = listOf(
        "wurmmodloader-core",
        "wurmmodloader-api",
        "wurmmodloader-modsupport",
        "wurmmodloader-legacy",
    )

    modules.forEach { module ->
        from(project(":$module").buildDir.resolve("libs")) {
            include("*.jar")
            exclude("*-sources.jar", "*-javadoc.jar")
            into(".")
        }
    }

    from("distribution") {
        include("javassist.jar", "gson.jar", "snakeyaml-2.2.jar")
        into(".")
    }

    from("distribution/scripts") {
        include(
            "wurmmodloader.sh", "wurmmodloader.bat",
            "wurmmodloader-rebuild-dbs.sh", "wurmmodloader-rebuild-dbs.bat",
            "wurmmodloader-create-world.sh", "wurmmodloader-create-world.bat"
        )
        fileMode = Integer.parseInt("755", 8)
        into(".")
    }

    from(".") {
        include("README.md", "LICENSE")
        into(".")
    }

    from("distribution/mods") {
        into("mods")
        includeEmptyDirs = true
    }
}

// =============================
// Developer / Full Bundle ZIP
// =============================
tasks.register<Zip>("dev") {
    group = "distribution"
    description = "Creates full developer ZIP including all subproject jars and docs"

    dependsOn("buildAll")

    archiveBaseName.set("WurmModloader-DevBundle")
    archiveVersion.set(version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    subprojects.forEach { proj ->
        val jarTask = proj.tasks.named("jar")
        from(jarTask.map { it.outputs.files }) {
            into("libs")
        }
    }

    // Extra runtime and helper libs
    from("distribution") {
        include("javassist.jar")
        into("libs")
    }

    // Docs and config
    from(".") {
        include("README.md", "LICENSE", "NOTICE.md")
        into(".")
    }

    from("distribution") {
        include("INSTALL.md", "logging.properties")
        into("config")
    }

    // Scripts
    from("distribution/scripts") {
        fileMode = Integer.parseInt("755", 8)
        into("scripts")
    }

    // Mods folder (empty)
    from("distribution/mods") {
        into("mods")
        includeEmptyDirs = true
    }
}
