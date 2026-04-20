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
    compileOnly(files("../../distribution/server.jar", "../../distribution/common.jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName.set("hellomod")
}

tasks.register<Zip>("modDistribution") {
    archiveBaseName.set("hellomod")
    archiveVersion.set(project.version.toString())

    from(tasks.jar) {
        into("mods/hellomod")
    }
    from("src/dist") {
        into("mods")
    }
}

tasks.build {
    dependsOn(tasks.named("modDistribution"))
}
