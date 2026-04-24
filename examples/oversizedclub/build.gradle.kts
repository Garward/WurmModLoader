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
    implementation(project(":wurmmodloader-modsupport"))

    compileOnly(files(
        "${rootProject.projectDir}/distribution/server.jar",
        "${rootProject.projectDir}/distribution/common.jar"
    ))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    jar {
        archiveBaseName.set("oversizedclub")
        archiveVersion.set("")

        manifest {
            attributes(
                "Implementation-Title" to "Oversized Club Mod",
                "Implementation-Version" to project.version,
                "Built-By" to "WurmModLoader",
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
            )
        }
    }
}
