plugins {
    id("java")
}

dependencies {
    compileOnly(files("${rootProject.projectDir}/lib/server.jar"))
    compileOnly(files("${rootProject.projectDir}/lib/common.jar"))

    compileOnly(project(":wurmmodloader-api"))
    compileOnly(project(":wurmmodloader-core"))

    compileOnly(files("${rootProject.projectDir}/lib/modlauncher-legacy.jar"))

    // SnakeYAML is bundled with the server runtime, so compileOnly is enough.
    compileOnly("org.yaml:snakeyaml:2.2")
}

tasks {
    jar {
        archiveBaseName.set("templarpatrol")
        archiveVersion.set("")
    }
}
