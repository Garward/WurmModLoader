plugins {
    java
}

group = "com.garward.wurmmodloader.mods"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":wurmmodloader-api"))
    compileOnly(project(":wurmmodloader-core"))
    compileOnly(project(":wurmmodloader-modsupport"))
    compileOnly(project(":wurmmodloader-legacy"))

    compileOnly(files(
        "${rootProject.projectDir}/distribution/server.jar",
        "${rootProject.projectDir}/distribution/common.jar"
    ))

    compileOnly(files("${rootProject.projectDir}/distribution/javassist.jar"))
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
        archiveBaseName.set("gmtools")
        archiveVersion.set("")
    }
}
