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

    compileOnly("org.gotti.wurmunlimited:common:${project.property("wurmVersion")}")
    compileOnly("org.gotti.wurmunlimited:server:${project.property("wurmVersion")}")

    compileOnly("org.yaml:snakeyaml:2.2")
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
