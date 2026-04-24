rootProject.name = "wurmmodloader"

// --- Local property overrides (gitignored) ---
// `gradle.properties.local` at this directory carries personal paths like
// wurmServerDir / wurmClientDir. Loaded into Gradle project properties so the
// build sees them just like committed gradle.properties entries.
val localProps = file("gradle.properties.local")
if (localProps.exists()) {
    val props = java.util.Properties()
    localProps.inputStream().use { props.load(it) }
    gradle.beforeProject {
        props.forEach { k, v -> extra[k.toString()] = v.toString() }
    }
}


include(
    "wurmmodloader-api",
    "wurmmodloader-core",
    "wurmmodloader-legacy",
    "wurmmodloader-modsupport",
    // "wurmmodloader-patcher",  // Legacy - not used anymore
    "wurmmodloader-cli"
)

// Example mods
include("examples:hellomod")
project(":examples:hellomod").projectDir = file("examples/hellomod")

include("examples:oversizedclub")
project(":examples:oversizedclub").projectDir = file("examples/oversizedclub")

include("examples:templatemod")
project(":examples:templatemod").projectDir = file("examples/templatemod")

include("examples:database-backend-noop")
project(":examples:database-backend-noop").projectDir = file("examples/database-backend-noop")

// Power Fantasy RPG mods
include("mods:materialsystem")
project(":mods:materialsystem").projectDir = file("mods/materialsystem")

include("mods:soulboundgear")
project(":mods:soulboundgear").projectDir = file("mods/soulboundgear")

include("mods:upgradetree")
project(":mods:upgradetree").projectDir = file("mods/upgradetree")

include("mods:powerscaling")
project(":mods:powerscaling").projectDir = file("mods/powerscaling")

include("mods:eventlister")
project(":mods:eventlister").projectDir = file("mods/eventlister")

include("mods:postgresbackend")
project(":mods:postgresbackend").projectDir = file("mods/postgresbackend")

include("mods:livemap")
project(":mods:livemap").projectDir = file("mods/livemap")

// Dormant — pivoted to mods/gmtools (in-world building tools > external blueprint import).
// See mods/blueprints/DORMANT.md. Re-enable by uncommenting if the import path is revisited.
// include("mods:blueprints")
// project(":mods:blueprints").projectDir = file("mods/blueprints")

include("mods:gmtools")
project(":mods:gmtools").projectDir = file("mods/gmtools")

include("mods:surfaceminingfix")
project(":mods:surfaceminingfix").projectDir = file("mods/surfaceminingfix")

// Note: Community mods now live in WurmModLoader-CommunityMods repo
// They depend on JARs from this repo's libs/ directory
