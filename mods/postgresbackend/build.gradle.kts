plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.garward.wurmmodloader.mods"
version = "1.0.0"

repositories {
    mavenCentral()
}

// pgjdbc is bundled alongside the mod jar (not on the server's classpath by default).
val driverRuntime: Configuration by configurations.creating

dependencies {
    implementation(project(":wurmmodloader-api"))

    // Embedded Postgres: manages a real Postgres process whose data dir lives
    // inside the server folder. Binaries are NOT shipped — downloaded lazily on
    // first boot via our own PgBinaryResolver (see LazyBinaryResolver.java).
    implementation("io.zonky.test:embedded-postgres:2.0.7") {
        // Bundled per-OS binaries add ~80MB; we download lazily via LazyBinaryResolver.
        exclude(group = "io.zonky.test.postgres")
    }

    // sqlite-jdbc for the in-process SQLite → Postgres importer. No extra config
    // needed from server owners; driver lives in the shaded mod jar.
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")

    // PostgreSQL JDBC driver — compileOnly for the source, plus driverRuntime
    // so modDistribution can copy it next to the mod jar.
    compileOnly("org.postgresql:postgresql:42.7.3")
    driverRuntime("org.postgresql:postgresql:42.7.3") {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    // The gotti maven publication (4596061, last updated 2023-11) is stale —
    // its Migrator$FlywayConfigurer declares configureMigrations() as default
    // instead of abstract, so PostgresMigrator's lambda doesn't compile against
    // it. Keep this mod on the local distribution jars and gate it on CI.
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))
}

// Skip compilation entirely if the gitignored vanilla Wurm jars aren't present
// (e.g. on CI). Locally these are shipped under distribution/.
val vanillaJarsPresent = file("../../distribution/server.jar").exists() &&
        file("../../distribution/common.jar").exists()

tasks.withType<JavaCompile>().configureEach { onlyIf { vanillaJarsPresent } }
tasks.withType<Javadoc>().configureEach { onlyIf { vanillaJarsPresent } }
tasks.withType<Jar>().configureEach { onlyIf { vanillaJarsPresent } }

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("postgresbackend")
    archiveVersion.set("")
    enabled = false  // defer to shadowJar — single fat jar is what the loader sees
}

tasks.shadowJar {
    archiveBaseName.set("postgresbackend")
    archiveVersion.set("")
    archiveClassifier.set("")
    mergeServiceFiles()
    // Keep the shadowJar free of pgjdbc — we ship that as a separate jar so
    // embedded and external modes both resolve the driver from the same place.
    dependencies {
        exclude(dependency("org.postgresql:postgresql"))
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("postgresbackend")
    archiveVersion.set(project.version.toString())

    from(tasks.shadowJar) {
        into("mods/postgresbackend")
    }
    from(driverRuntime) {
        into("mods/postgresbackend")
    }
    from("src/dist") {
        into("mods/postgresbackend")
        filesMatching("**/migrate.sh") { mode = 0b111_101_101 /* 0755 */ }
        filesMatching("**/connect.sh") { mode = 0b111_101_101 /* 0755 */ }
        exclude("**/.venv/**")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
