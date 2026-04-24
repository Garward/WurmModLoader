plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// =============================
// Uber JAR (server-owner runtime)
// =============================
// Bundles core + api + modsupport + legacy + cli + javassist + gson + snakeyaml
// into one jar. Unrelocated: mods depend on real javassist.* class names.
dependencies {
    implementation(project(":wurmmodloader-api"))
    implementation(project(":wurmmodloader-core"))
    implementation(project(":wurmmodloader-modsupport"))
    implementation(project(":wurmmodloader-legacy"))
    implementation(project(":wurmmodloader-cli"))
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("wurmmodloader")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to "com.garward.wurmmodloader.serverlauncher.ServerLauncher",
            "Implementation-Title" to "WurmModLoader Runtime",
            "Implementation-Version" to project.version
        )
    }
    // Drop per-module manifests and signatures to avoid SecurityException at runtime
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// The root project doesn't ship its own classes, so disable the thin jar.
tasks.named<Jar>("jar") {
    enabled = false
}

allprojects {
    group = "com.garward.wurmmodloader"
    version = "0.10.0"

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
// Server-owner Runtime ZIP (single uber jar)
// =============================
tasks.register<Zip>("dist") {
    group = "distribution"
    description = "Server-owner runtime: single uber jar + launcher scripts + bundled mods"

    dependsOn("shadowJar", "buildAll")

    archiveBaseName.set("WurmModloader-Runtime")
    archiveVersion.set(version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Uber jar (everything bundled)
    from(tasks.named("shadowJar")) {
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

    from("distribution/config") {
        into("config")
        includeEmptyDirs = true
    }

    // ---- Bundled near-finished mods ----
    // livemap: enabled by default, drop-in web map.
    from(project(":mods:livemap").buildDir.resolve("libs/livemap.jar")) {
        into("mods/livemap")
    }
    from("mods/livemap/src/dist") {
        into("mods")
    }

    // gmtools: enabled by default, in-game admin toolkit.
    from(project(":mods:gmtools").buildDir.resolve("libs/gmtools.jar")) {
        into("mods/gmtools")
    }
    from("mods/gmtools/src/dist") {
        into("mods")
    }

    // postgresbackend: DISABLED by default — requires Postgres setup.
    // Ship the jar + pgjdbc driver + scripts but rename the properties to
    // .disabled so the modloader skips it. Server owners rename to enable.
    from(project(":mods:postgresbackend").buildDir.resolve("libs/postgresbackend.jar")) {
        into("mods/postgresbackend")
    }
    from(project(":mods:postgresbackend").configurations.getByName("driverRuntime")) {
        include("postgresql-*.jar")
        into("mods/postgresbackend")
    }
    from("mods/postgresbackend/src/dist") {
        into("mods")
        exclude("**/.venv/**", "postgresbackend.properties")
        filesMatching("**/*.sh") { mode = Integer.parseInt("755", 8) }
    }
    from("mods/postgresbackend/src/dist/postgresbackend.properties") {
        into("mods")
        rename { "postgresbackend.properties.disabled" }
    }
}

// =============================
// Modder SDK ZIP (separate module jars + sources/javadocs)
// =============================
tasks.register<Zip>("sdk") {
    group = "distribution"
    description = "Modder SDK: individual module jars + sources + javadocs"

    dependsOn("buildAll")

    archiveBaseName.set("WurmModloader-SDK")
    archiveVersion.set(version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val sdkModules = listOf(
        "wurmmodloader-api",
        "wurmmodloader-core",
        "wurmmodloader-modsupport",
        "wurmmodloader-legacy",
    )

    sdkModules.forEach { module ->
        from(project(":$module").buildDir.resolve("libs")) {
            include("*.jar")
            into("libs")
        }
    }

    from(".") {
        include("README.md", "LICENSE", "NOTICE.md")
        into(".")
    }

    from("distribution") {
        include("INSTALL.md")
        into(".")
    }

    from("distribution/config") {
        into("config")
        includeEmptyDirs = true
    }
}

// Legacy alias — keep `dev` working for any external tooling still calling it.
tasks.register("dev") {
    group = "distribution"
    description = "Deprecated alias for :sdk"
    dependsOn("sdk")
}
