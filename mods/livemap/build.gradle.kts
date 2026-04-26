plugins {
    java
}

group = "com.garward.wurmmodloader.mods"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // WurmModLoader framework — project references so source changes flow through.
    compileOnly(project(":wurmmodloader-api"))
    compileOnly(project(":wurmmodloader-core"))
    compileOnly(project(":wurmmodloader-legacy"))

    // Wurm server JARs
    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")

    // Javassist (shipped with the server distribution)
    compileOnly("org.javassist:javassist:${project.property("javassistVersion")}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.release.set(8)
}

tasks {
    jar {
        archiveBaseName.set("livemap")
        archiveVersion.set("")
    }
}
