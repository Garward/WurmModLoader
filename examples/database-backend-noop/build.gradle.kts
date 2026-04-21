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
    // server.jar / common.jar are only referenced at compile time (for ConnectionFactory,
    // WurmDatabaseSchema, MigrationStrategy, MigrationResult, MigrationVersion).
    // The live server-side classloader provides them at runtime.
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))
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
