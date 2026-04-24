plugins {
    id("java")
}

dependencies {
    compileOnly(files("${rootProject.projectDir}/lib/server.jar"))
    compileOnly(files("${rootProject.projectDir}/lib/common.jar"))

    compileOnly(project(":wurmmodloader-api"))
    compileOnly(project(":wurmmodloader-core"))

    compileOnly(files("${rootProject.projectDir}/lib/modlauncher-legacy.jar"))
}

tasks {
    jar {
        archiveBaseName.set("surfaceminingfix")
        archiveVersion.set("")
    }
}
