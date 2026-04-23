package com.garward.wurmmodloader.core.worldseed;

import com.garward.wurmmodloader.api.events.server.WorldSeedResult;
import com.garward.wurmmodloader.api.worldseed.WorldSeedAPI;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.VillageRole;
import com.wurmonline.server.villages.Villages;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs at {@code ServerStartedEvent} time, after the vanilla runtime is up
 * and {@link com.garward.wurmmodloader.core.worldseed.WorldSeedBootstrap} has
 * published its picked tile via {@link WorldSeedAPI}.
 *
 * <p>If the boot was SEEDED and {@link WorldSeedConfig#createVillage} is true,
 * creates a starter village end-to-end through vanilla APIs:
 * <ol>
 *   <li>{@link ItemFactory#createItem} produces a settlement deed item
 *       (template 663) at the picked tile, system-owned (creator id {@code -10L}).</li>
 *   <li>{@link Creature#doNew} spawns a headless founder NPC (template 113)
 *       at the same tile — {@code createVillage} needs a live {@link Creature}
 *       for {@code addCitizen} / achievement / history calls.</li>
 *   <li>{@link Villages#createVillage} does the rest — VILLAGES row,
 *       token item, InfluenceChain, upkeep, CITIZENS entry.</li>
 *   <li>Optional altar items adjacent (template 327 Altar of Three + 328 Bone
 *       Altar by default, configurable).</li>
 *   <li>Optionally despawns the founder NPC ({@code despawnFounderNpc=true}
 *       keeps the village tidy; the founder name string persists in the
 *       VILLAGES row).</li>
 * </ol>
 *
 * <p>Power-user path: set {@code createVillage=false} and subscribe to
 * {@link com.garward.wurmmodloader.api.events.server.WorldSeededEvent} from a
 * mod. The framework still picks the tile and writes SERVERS spawn points;
 * the mod builds the physical town however it likes.
 *
 * <p>All DB writes go through vanilla code → backend-agnostic (sqlite, mysql,
 * postgres via the DatabaseBackend SPI).
 */
public final class WorldSeedCompletionHandler {

    private static final Logger LOGGER = Logger.getLogger(WorldSeedCompletionHandler.class.getName());

    /** Generic human NPC — {@code Creature.doNew} treats 1 / 113 as Npc subclass. */
    private static final int FOUNDER_TEMPLATE = 113;
    /** Freedom Isles — the WU peaceful home kingdom. Used if we can't read the live server's KINGDOM. */
    private static final byte FALLBACK_KINGDOM = 4;

    /**
     * Reads {@code Servers.localServer.KINGDOM} via reflection so the founder NPC
     * and starter village match whatever kingdom the SERVERS row is set to —
     * Freedom (4) on PvE home, Jenn-Kellon (1) on Chaos. Falls back to Freedom
     * if the reflection path fails.
     */
    private static byte resolveKingdom() {
        try {
            Class<?> serversClass = Class.forName("com.wurmonline.server.Servers");
            Object local = serversClass.getField("localServer").get(null);
            if (local != null) {
                byte k = local.getClass().getField("KINGDOM").getByte(local);
                if (k > 0) return k;
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Could not read Servers.localServer.KINGDOM — defaulting to Freedom ("
                    + FALLBACK_KINGDOM + "). " + t.getMessage());
        }
        return FALLBACK_KINGDOM;
    }

    private WorldSeedCompletionHandler() {}

    /** Called from {@code ServerHook.fireOnServerStarted} before the WorldSeededEvent post. */
    public static void run() {
        WorldSeedResult r = WorldSeedAPI.getResult();
        if (r == null) {
            return;
        }
        WorldSeedConfig cfg = WorldSeedConfigLoader.load();

        // Idempotent founder sweep: on any boot after a seed — fresh SEEDED run,
        // or a subsequent SKIPPED_ALREADY_SEEDED boot — wipe any leftover founder
        // NPCs at the primary village. Previous boots may have failed to despawn
        // (crash, template lookup miss, exception swallowed), leaving the named
        // NPC standing in the starter town forever. Cheap to re-run every boot.
        if (cfg.despawnFounderNpc) {
            sweepStaleFounders(cfg);
        }

        if (r.getOutcome() != WorldSeedResult.Outcome.SEEDED) {
            return;
        }
        if (!cfg.createVillage) {
            LOGGER.info("[WorldSeed] createVillage=false — skipping vanilla village creation. "
                + "Mods consuming WorldSeededEvent can materialize their own town at tile ("
                + r.getCenterTileX() + ", " + r.getCenterTileY() + ").");
            return;
        }

        int tileX = r.getCenterTileX();
        int tileY = r.getCenterTileY();
        float meterX = (tileX << 2) + 2.0f;
        float meterY = (tileY << 2) + 2.0f;
        int half = Math.max(1, cfg.centerTownHalfSize);

        // Idempotency: skip re-creation on force-boots after an initial seed.
        Village existing = Villages.getVillage(tileX, tileY, true);
        if (existing != null) {
            LOGGER.info("[WorldSeed] Village '" + existing.getName() + "' (id=" + existing.getId()
                + ") already exists at tile (" + tileX + ", " + tileY + ") — skipping creation."
                + " SERVERS spawn points were still refreshed at PreInit.");
            return;
        }

        Creature founder = null;
        try {
            Item deed = ItemFactory.createItem(
                ItemList.settlementDeed, 99.0f, meterX, meterY, 180.0f,
                true, (byte) 0, -10L, null);
            LOGGER.info("[WorldSeed] Deed item created id=" + deed.getWurmId()
                + " at tile (" + tileX + ", " + tileY + ").");

            byte kingdom = resolveKingdom();
            LOGGER.info("[WorldSeed] Using kingdom=" + kingdom
                + " for founder NPC + village (from Servers.localServer.KINGDOM).");

            founder = Creature.doNew(
                FOUNDER_TEMPLATE, false, meterX, meterY, 0.0f, 0,
                cfg.founderNpcName, (byte) 0, kingdom, (byte) 0, false, (byte) 0, 0);
            LOGGER.info("[WorldSeed] Founder NPC spawned id=" + founder.getWurmId()
                + " name='" + founder.getName() + "'.");

            Village village = Villages.createVillage(
                tileX - half, tileX + half, tileY - half, tileY + half,
                tileX, tileY,
                cfg.centerTownName, founder, deed.getWurmId(),
                true, true, "Welcome, traveler",
                cfg.centerTownPermanent, kingdom, 5);
            LOGGER.info("[WorldSeed] Village created id=" + village.getId()
                + " name='" + village.getName()
                + "' footprint=(" + (tileX - half) + "," + (tileY - half)
                + ")→(" + (tileX + half) + "," + (tileY + half) + ").");

            // Realign SERVERS spawn to the actual created village token — covers the
            // case where PreInit wrote spawn to the picked tile but createVillage
            // placed the token at a slightly-different coord (token gets its own item).
            int tokenX = village.getTokenX();
            int tokenY = village.getTokenY();
            if (tokenX != tileX || tokenY != tileY) {
                LOGGER.info("[WorldSeed] Village token landed at (" + tokenX + ", " + tokenY
                    + ") — realigning SERVERS spawn from picked tile (" + tileX + ", " + tileY + ").");
                SpawnPointWriter.updateAllKingdoms(tokenX, tokenY);
            }

            placeAltars(cfg, tileX, tileY);

            if (cfg.starterTownPublicAccess) {
                configureEverybodyRole(village);
            }

            // Backfill STRUCTURES.VILLAGE for rows written pre-init by the
            // starter-town importer (it runs before the village exists, so
            // the FK column starts at -1 and gets stamped here).
            StarterTownImporter.backfillVillageId(village.getId());

            if (cfg.despawnFounderNpc) {
                try {
                    founder.delete();
                    LOGGER.info("[WorldSeed] Founder NPC despawned — village keeps founder name string.");
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING,
                        "[WorldSeed] Could not despawn founder NPC — " + t.getMessage(), t);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE,
                "[WorldSeed] Village creation failed at tile (" + tileX + ", " + tileY + ") — " + t.getMessage(), t);
        }
    }

    /**
     * Widen the non-citizen (Everybody) role on the seeded starter village so
     * visitors can actually interact with the town without being treated as
     * trespassers. Grants friendly-visitor perms (pass gates, lead/tame, cut
     * grass, pick sprouts, harvest fruit/fields, forage, pull/push, drop,
     * pick up own items) but deliberately leaves build/terraform/destroy/
     * manage/invite/expand/lock perms OFF — outsiders cannot take control of
     * the town.
     *
     * <p>Uses reflection to read the package-private {@code Village.everybody}
     * field, which is populated during {@link Villages#createVillage}.
     */
    private static void configureEverybodyRole(Village village) {
        try {
            Field f = Village.class.getDeclaredField("everybody");
            f.setAccessible(true);
            Object role = f.get(village);
            if (role == null) {
                // Fall back to creating it explicitly.
                Method m = Village.class.getDeclaredMethod("createRoleEverybody");
                m.setAccessible(true);
                role = m.invoke(village);
            }
            if (!(role instanceof VillageRole)) {
                LOGGER.warning("[WorldSeed] Could not resolve everybody role — skipping perms setup.");
                return;
            }
            VillageRole er = (VillageRole) role;

            // Allow — friendly-visitor interactions for a public starter town.
            er.setCanPassGates(true);
            er.setCanLead(true);
            er.setCanDigResource(true);       // dig clay / tar / peat / moss
            er.setCanForageBotanize(true);    // forage / botanize / investigate
            er.setCanDrop(true);
            er.setCanPullPushTurn(true);
            er.setCanImproveRepair(true);
            er.setCanMineIron(true);
            er.setCanMineOther(true);

            // Deny — anything that grants town control or destructive capability.
            er.setCanBuild(false);
            er.setCanPlanBuildings(false);
            er.setCanTerraform(false);
            er.setCanPave(false);
            er.setCanDestroyFence(false);
            er.setCanDestroyItems(false);
            er.setCanDestroyAnyBuilding(false);
            er.setCanAttachLocks(false);
            er.setCanPickLocks(false);
            er.setCanPickupPlanted(false);
            er.setCanPlaceMerchants(false);
            er.setCanManageRoles(false);
            er.setCanManageCitizenRoles(false);
            er.setCanManageAllowedObjects(false);
            er.setCanManageGuards(false);
            er.setCanManageMap(false);
            er.setCanManageReputations(false);
            er.setCanManageSettings(false);
            er.setCanInviteCitizens(false);
            er.setCanResizeSettlement(false);
            er.setCanAttackCitizens(false);

            er.save();
            LOGGER.info("[WorldSeed] Everybody role widened for starter town — pass/lead/dig-clay/forage/drop/pull/improve/mine-veins allowed, build/manage/destroy denied.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[WorldSeed] Failed to configure Everybody role — " + t.getMessage(), t);
        }
    }

    /**
     * Delete any non-player creatures whose name matches the configured founder
     * name. Only touches NPCs on starter-kingdom villages — avoids nuking legit
     * player-renamed mobs or NPCs unrelated to the seeder.
     */
    private static void sweepStaleFounders(WorldSeedConfig cfg) {
        String target = cfg.founderNpcName;
        if (target == null || target.isEmpty()) return;
        try {
            Creature[] all = Creatures.getInstance().getCreatures();
            int deleted = 0;
            for (Creature c : all) {
                if (c == null) continue;
                try {
                    if (c.isPlayer()) continue;
                    if (!target.equalsIgnoreCase(c.getName())) continue;
                    c.delete();
                    deleted++;
                } catch (Throwable perCre) {
                    LOGGER.log(Level.WARNING,
                        "[WorldSeed] Could not despawn leftover founder '" + target
                            + "' id=" + safeId(c) + " — " + perCre.getMessage(), perCre);
                }
            }
            if (deleted > 0) {
                LOGGER.info("[WorldSeed] Swept " + deleted + " leftover '" + target
                    + "' NPC(s) from prior boots — village keeps founder name string in VILLAGES row.");
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                "[WorldSeed] Stale-founder sweep failed — " + t.getMessage(), t);
        }
    }

    private static long safeId(Creature c) {
        try { return c.getWurmId(); } catch (Throwable ignored) { return -1L; }
    }

    private static void placeAltars(WorldSeedConfig cfg, int tileX, int tileY) {
        if (cfg.centerAltars == null || cfg.centerAltars.isEmpty()) return;
        int offset = 2;
        for (String altarName : cfg.centerAltars) {
            int tpl = resolveAltarTemplate(altarName);
            if (tpl < 0) {
                LOGGER.warning("[WorldSeed] Unknown altar '" + altarName + "' — skipping.");
                continue;
            }
            int ax = tileX + offset;
            int ay = tileY;
            try {
                Item altar = ItemFactory.createItem(
                    tpl, 99.0f, (ax << 2) + 2.0f, (ay << 2) + 2.0f, 0.0f,
                    true, (byte) 0, -10L, null);
                LOGGER.info("[WorldSeed] Altar '" + altarName + "' placed id=" + altar.getWurmId()
                    + " at tile (" + ax + ", " + ay + ").");
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[WorldSeed] Failed to place altar '" + altarName + "' — " + t.getMessage(), t);
            }
            offset += 2;
        }
    }

    private static int resolveAltarTemplate(String name) {
        if (name == null) return -1;
        String n = name.trim().toLowerCase();
        switch (n) {
            case "altar_of_three":
            case "altarofthree":
            case "altarholy":
                return ItemList.altarHoly;
            case "bone_altar":
            case "bonealtar":
            case "altarunholy":
                return ItemList.altarUnholy;
            case "altar_wood":   return ItemList.altarWood;
            case "altar_stone":  return ItemList.altarStone;
            case "altar_gold":   return ItemList.altarGold;
            case "altar_silver": return ItemList.altarSilver;
            default:
                try { return Integer.parseInt(n); } catch (NumberFormatException ignored) { return -1; }
        }
    }
}
