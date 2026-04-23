package com.garward.wurmmodloader.modsupport.actions.area;

import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionDef;
import com.garward.wurmmodloader.api.events.eventlogic.area.AreaActionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Convenience builder: given a set of radius/skill tiers, constructs and
 * registers an {@link AreaActionPerformer} for each tier, grouped by action
 * type. Hand the resulting map to {@link AreaActionMenus} to populate menus.
 */
public final class AreaActionRegistrar {

    private final Map<AreaActionType, List<AreaActionPerformer>> performers = new LinkedHashMap<>();
    private final Consumer<AreaActionPerformer> onRegister;

    public AreaActionRegistrar() {
        this(null);
    }

    /**
     * @param onRegister optional hook fired once per created performer (e.g.
     *                   to register its action id in a mounted-action
     *                   allowlist).
     */
    public AreaActionRegistrar(Consumer<AreaActionPerformer> onRegister) {
        this.onRegister = onRegister;
    }

    public AreaActionRegistrar register(AreaActionType type, List<AreaActionDef> tiers) {
        if (tiers == null || tiers.isEmpty()) return this;
        List<AreaActionPerformer> built = new ArrayList<>(tiers.size());
        for (AreaActionDef def : tiers) {
            built.add(new AreaActionPerformer(type, def.level, def.radius, onRegister));
        }
        performers.put(type, built);
        return this;
    }

    public Map<AreaActionType, List<AreaActionPerformer>> performers() {
        return performers;
    }
}
