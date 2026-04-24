package com.garward.wurmmodloader.mods.surfaceminingfix;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.structure.SurfaceMiningSlopeLowerCheckEvent;
import com.garward.wurmmodloader.api.events.structure.SurfaceMiningSurroundCheckEvent;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

/**
 * SurfaceMiningFix — minimal port of bdew/SurfaceMiningFixMod.
 *
 * <p>Exposes two config switches that consume framework events landed in
 * the WurmModLoader v0.10.x line:</p>
 *
 * <ul>
 *   <li>{@code alwaysLowerRockSlope} — override
 *       {@link SurfaceMiningSlopeLowerCheckEvent} to force-lower the slope
 *       every swing, bypassing the {@code Server.rand.nextFloat() < chance}
 *       roll inside {@code TileRockBehaviour.mine}.</li>
 *   <li>{@code noNeedToUnconverRock} — override
 *       {@link SurfaceMiningSurroundCheckEvent} so the "surrounding area
 *       needs to be rock before you mine" abort is suppressed.</li>
 * </ul>
 *
 * <p>The upstream Azbantium Fist enchant, Azbantium Pickaxe item, and
 * Seafloor Mining Rig item are not in this port — they require spell/item
 * registration in {@code com.wurmonline.server.*} packages and a
 * water-depth event the framework hasn't yet absorbed. Track those as a
 * follow-up on the SurfaceMiningFix port.</p>
 */
public class SurfaceMiningFixMod implements WurmServerMod, Configurable {

    private static final Logger LOGGER = Logger.getLogger(SurfaceMiningFixMod.class.getName());

    private boolean alwaysLowerRockSlope = false;
    private boolean noNeedToUnconverRock = false;
    private boolean debug = false;

    @Override
    public void configure(Properties properties) {
        alwaysLowerRockSlope = parseBool(properties, "alwaysLowerRockSlope", false);
        noNeedToUnconverRock = parseBool(properties, "noNeedToUnconverRock", false);
        debug                = parseBool(properties, "debug", false);

        LOGGER.log(Level.INFO,
            "[SurfaceMiningFix] config — alwaysLowerRockSlope={0}, noNeedToUnconverRock={1}, debug={2}",
            new Object[] { alwaysLowerRockSlope, noNeedToUnconverRock, debug });
    }

    @SubscribeEvent
    public void onSlopeLowerCheck(SurfaceMiningSlopeLowerCheckEvent event) {
        if (alwaysLowerRockSlope) {
            event.setOverride(Boolean.TRUE);
            if (debug) {
                LOGGER.info("[SurfaceMiningFix] forcing slope-lower override TRUE"
                    + " (natural chance was " + event.getNaturalChance() + ")");
            }
        }
    }

    @SubscribeEvent
    public void onSurroundCheck(SurfaceMiningSurroundCheckEvent event) {
        if (noNeedToUnconverRock) {
            event.setOverride(Boolean.TRUE);
            if (debug) {
                LOGGER.info("[SurfaceMiningFix] bypassing surround-rock requirement");
            }
        }
    }

    private static boolean parseBool(Properties props, String key, boolean def) {
        String raw = props.getProperty(key);
        if (raw == null) return def;
        raw = raw.trim();
        return raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("yes") || raw.equals("1");
    }
}
