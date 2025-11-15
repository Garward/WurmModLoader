# Icon System Quick Reference

**Status:** ✅ Working implementation (alchemy mod verified)
**Last Updated:** 2025-11-06

## Critical Discovery: Append to Vanilla Sheets

**⚠️ KEY FINDING:** Custom icons are **NOT** placed on new sheets (7+). They are **COMPOSITED onto existing vanilla sheets** at empty positions.

### Why This Works
- Client only loads vanilla sheets (icons.png, misc.png, resource.png, tools.png, armor.png, weapons.png, resource2.png)
- Custom icons fill empty slots in these sheets
- Modified sheets are distributed via httpserver/serverpacks
- Client sees "vanilla" sheets with custom icons embedded

---

## Working Implementation Pattern (Alchemy Mod)

### 1. Register Custom Icons

```java
import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.icon.IconRegistry;

// In your mod's onItemTemplatesCreated() or similar
Icon icon = IconRegistry.registerCustom(
    new ResourceLocation("mymod", "icon_name"),
    "icon_file.png"  // Just the filename, no path
);

short iconId = (short) icon.getIconId();  // Use in ItemTemplateBuilder
```

**Example from alchemy:**
```java
Icon mouldClayIconObj = IconRegistry.registerCustom(
    new ResourceLocation("alchemy", "mould_clay"),
    "phialMouldClay.png"
);
mouldClayIcon = (short) mouldClayIconObj.getIconId();  // Returns 1501

// Later in item creation:
builder.imageNumber(mouldClayIcon)
```

### 2. Icon File Placement

**External Files (Recommended):**
```
mods/mymod/icons/icon_file.png
```

**JAR Resources (Alternative):**
```
mymod.jar/icons/icon_file.png
```

**Icon Specs:**
- Format: PNG with alpha transparency
- Size: 64×64 pixels (will be scaled to 32×32 by generator)
- Color: RGB or RGBA

### 3. Automatic Pack Generation

Server automatically:
1. Loads vanilla sheets from graphics.jar
2. Composites custom icons onto vanilla sheets at allocated positions
3. Saves modified sheets to `httpserver/iconpacks/`
4. Creates icon_manifest.json (optional, for debugging)
5. Packages into iconpack.jar for serverpacks distribution

**No manual pack generation needed!**

---

## Icon ID Allocation

### Vanilla Sheets (0-1679)
| Sheet | File | Icon IDs | Notes |
|-------|------|----------|-------|
| 0 | icons.png | 0-239 | UI elements, body parts |
| 1 | misc.png | 240-479 | Containers, liquids |
| 2 | resource.png | 480-719 | Ores, logs, crops |
| 3 | tools.png | 720-959 | Tools, implements |
| 4 | armor.png | 960-1199 | Armor, shields |
| 5 | weapons.png | 1200-1439 | Weapons |
| 6 | resource2.png | 1440-1679 | Additional materials |

### Custom Icons (Auto-Allocated)
- **First custom ID:** 1680 (but actually fills empty slots in sheet 6 first!)
- **Actual allocation:** System finds empty positions in vanilla sheets
- **Alchemy example:** Icons 1501-1530 (sheet 6, row 3-4)

**Formula:**
```java
int sheetIndex = iconId / 240;      // Which sheet (0-6 for vanilla)
int position = iconId % 240;        // Position in sheet (0-239)
int row = position / 20;            // Row (0-11)
int column = position % 20;         // Column (0-19)
int xOffset = column * 32;          // Pixel X
int yOffset = row * 32;             // Pixel Y
```

---

## Key Code Locations

### Registration API
```
wurmmodloader-api/src/main/java/com/garward/wurmmodloader/api/icon/
├── Icon.java          # Icon metadata class
└── IconType.java      # Enum: VANILLA, CUSTOM, PLACEHOLDER, GENERATED
```

### Implementation
```
wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/icon/
├── IconRegistry.java        # Helper for icon registration
└── IconPackGenerator.java   # Composites icons onto vanilla sheets
```

### Integration
```
wurmmodloader-core/src/main/java/com/garward/wurmmodloader/core/registry/
└── Registries.java   # Global registry including ICONS
```

---

## Complete Example (Material Icons for Power Fantasy)

```java
package com.garward.wurmmodloader.mods.materialsystem;

import com.garward.wurmmodloader.api.event.ItemTemplatesCreatedEvent;
import com.garward.wurmmodloader.api.event.SubscribeEvent;
import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.icon.IconRegistry;
import com.garward.wurmmodloader.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

public class MaterialSystemMod implements WurmServerMod {

    private static short ifritCoreIcon;
    private static short frozenHeartIcon;

    @SubscribeEvent
    public void onItemTemplatesCreated(ItemTemplatesCreatedEvent event) {
        // Register icons
        Icon ifritIcon = IconRegistry.registerCustom(
            new ResourceLocation("powerfantasy", "ifrit_core"),
            "ifrit_core.png"
        );
        ifritCoreIcon = (short) ifritIcon.getIconId();

        Icon frozenIcon = IconRegistry.registerCustom(
            new ResourceLocation("powerfantasy", "frozen_heart"),
            "frozen_heart.png"
        );
        frozenHeartIcon = (short) frozenIcon.getIconId();

        // Create items using icons
        ItemTemplate ifritCore = new ItemTemplateBuilder(
                new ResourceLocation("powerfantasy", "ifrit_core"))
            .name("Ifrit Core", "Ifrit Cores", "A blazing core from a Fire Titan")
            .imageNumber(ifritCoreIcon)  // Use custom icon
            .weightGrams(500)
            .build();

        ItemTemplate frozenHeart = new ItemTemplateBuilder(
                new ResourceLocation("powerfantasy", "frozen_heart"))
            .name("Frozen Heart", "Frozen Hearts", "An icy heart from an Ice Titan")
            .imageNumber(frozenHeartIcon)  // Use custom icon
            .weightGrams(500)
            .build();
    }
}
```

### File Structure for Above
```
mods/powerfantasy/
├── icons/
│   ├── ifrit_core.png       # 64x64 PNG
│   └── frozen_heart.png     # 64x64 PNG
├── powerfantasy.jar
└── powerfantasy.properties
```

---

## Pack Generation Process (Automatic)

### When It Runs
- After `onItemTemplatesCreated()` event fires
- Before server marks as "ready"
- Once per server startup

### What It Does
```java
// Pseudocode of IconPackGenerator.generateIconPacks()

1. Load vanilla graphics.jar sheets (icons.png, misc.png, etc.)
   └─ Creates 7 BufferedImages (640x384 each, 32x32 icons)

2. For each custom icon:
   - Calculate position: iconId 1501 → sheet 6, row 3, column 1
   - Load custom PNG from mods/alchemy/icons/phialMouldClay.png
   - Composite onto vanilla sheet at calculated pixel offset
   - Clear rect at (32, 96) and draw 32x32 icon

3. Save all 7 modified sheets:
   └─ httpserver/iconpacks/resource2.png (contains custom icons)

4. Create icon_manifest.json (debugging aid)

5. Package into iconpack.jar for serverpacks distribution
```

### Output Structure
```
httpserver/iconpacks/
├── icons.png           # Modified sheet 0
├── misc.png            # Modified sheet 1
├── resource.png        # Modified sheet 2
├── tools.png           # Modified sheet 3
├── armor.png           # Modified sheet 4
├── weapons.png         # Modified sheet 5
├── resource2.png       # Modified sheet 6 (contains alchemy icons!)
├── icon_manifest.json  # Debug info
└── iconpack.jar        # For serverpacks distribution
```

---

## Troubleshooting

### Icons Don't Show in Client

**Check 1: Icon registered correctly?**
```bash
# Server logs should show:
INFO: Registered custom icon: alchemy:mould_clay (ID: 1501)
```

**Check 2: Icon file found?**
```bash
# Server logs should show:
FINE: Loaded icon from external file: mods/alchemy/icons/phialMouldClay.png
```

**Check 3: Pack generated?**
```bash
ls -la httpserver/iconpacks/
# Should see: resource2.png, icon_manifest.json, iconpack.jar
```

**Check 4: Client has packs?**
```bash
# Client should download from httpserver via serverpacks
# Check client logs for icon pack downloads
```

### Wrong Icon Displayed

**Likely cause:** Icon ID conflict or wrong sheet
```bash
# Check icon_manifest.json:
grep "icon_name" httpserver/iconpacks/icon_manifest.json

# Verify iconId, sheet, row, column match expectations
```

### Icon Looks Wrong (Stretched/Blurry)

**Likely cause:** Icon not 64×64 pixels
```bash
file mods/mymod/icons/icon.png
# Should show: PNG image data, 64 x 64, 8-bit/color RGBA
```

---

## Migration from ICONZZ

### Old Code (ICONZZ)
```java
import org.tyoda.wurm.Iconzz.Iconzz;

short iconId = Iconzz.getInstance().addIcon(
    "mymod.icon_name",
    "mods/mymod/icons/icon.png"
);
```

### New Code (WurmModLoader Icon Registry)
```java
import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.icon.IconRegistry;

Icon icon = IconRegistry.registerCustom(
    new ResourceLocation("mymod", "icon_name"),
    "icon.png"  // No path needed
);
short iconId = (short) icon.getIconId();
```

**Note:** Old ICONZZ code still works via compatibility shim!

---

## Performance Notes

- **Generation time:** ~100-500ms for 30 icons
- **Memory:** ~10MB peak during generation
- **Disk:** ~20-50KB per modified sheet
- **Server impact:** One-time cost at startup only
- **Client impact:** Downloads modified sheets once, cached locally

---

## DO's and DON'Ts

✅ **DO:**
- Use ResourceLocation for namespacing
- Place icons in `mods/modname/icons/`
- Use 64×64 PNG format
- Let system auto-allocate IDs
- Test with alchemy mod first

❌ **DON'T:**
- Hardcode icon IDs (use registry!)
- Assume icons go to sheet 7+
- Use non-PNG formats
- Use icons smaller than 64×64
- Manually edit vanilla sheets

---

## Quick Debug Checklist

```bash
# 1. Icon registered?
grep "Registered custom icon" logs/modloader*.log

# 2. Icon file loaded?
grep "Loaded icon from" logs/modloader*.log

# 3. Pack generated?
ls -la httpserver/iconpacks/

# 4. Check manifest
cat httpserver/iconpacks/icon_manifest.json

# 5. Verify sheet has icons
file httpserver/iconpacks/resource2.png
# Should be larger than vanilla if icons added
```

---

**Working Example:** Alchemy mod - 30 icons successfully using this system
**Tested:** Icon IDs 1501-1530, sheet 6, rows 3-4
**Distribution:** Via httpserver + serverpacks (automatic client download)
