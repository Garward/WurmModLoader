plugins {
    id("java")
}

dependencies {
    // Wurm server dependencies
    compileOnly(files("${rootProject.projectDir}/lib/server.jar"))
    compileOnly(files("${rootProject.projectDir}/lib/common.jar"))

    // WurmModLoader API and core
    compileOnly(project(":wurmmodloader-api"))
    compileOnly(project(":wurmmodloader-core"))

    // Legacy modloader interfaces
    compileOnly(files("${rootProject.projectDir}/lib/modlauncher-legacy.jar"))
}

tasks {
    jar {
        archiveBaseName.set("eventlister")
    }
}
