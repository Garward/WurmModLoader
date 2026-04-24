plugins {
    java
}

group = "com.garward.wurmmodloader.mods"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":wurmmodloader-api"))
    compileOnly(project(":wurmmodloader-core"))
    compileOnly(project(":wurmmodloader-legacy"))

    compileOnly(files(
        "${rootProject.projectDir}/distribution/server.jar",
        "${rootProject.projectDir}/distribution/common.jar"
    ))

    compileOnly(files("${rootProject.projectDir}/distribution/snakeyaml-2.2.jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    jar {
        archiveBaseName.set("templarpatrol")
        archiveVersion.set("")
    }
}
