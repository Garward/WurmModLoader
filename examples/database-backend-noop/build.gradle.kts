plugins {
    java
}

group = "com.garward.wurmmodloader.examples"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":wurmmodloader-api"))
    // ConnectionFactory / WurmDatabaseSchema / MigrationStrategy /
    // MigrationResult / MigrationVersion live in the vanilla Wurm jars, pulled
    // from the gotti.no-ip.org maven repo at compile time. The live server-side
    // classloader provides them at runtime.
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")
    // Flyway is shaded into vanilla server.jar (Wurm bundles a 2016-vintage copy).
    // The gotti maven artifact strips it out, so pull a normal copy at compile
    // time — the running server's classloader still provides Wurm's shaded version.
    compileOnly("org.flywaydb:flyway-core:5.2.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("database-backend-noop")
}

tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("database-backend-noop")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/database-backend-noop")
    }
    from("src/dist") {
        into("mods")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
