rootProject.name = "wurmmodloader"

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

// Note: Community mods now live in WurmModLoader-CommunityMods repo
// They depend on JARs from this repo's libs/ directory
