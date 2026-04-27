package com.garward.wurmmodloader.mods.upgradetree.ui;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.mods.declarativeui.api.WidgetNode;
import com.garward.wurmmodloader.mods.declarativeui.api.Widgets;
import com.garward.wurmmodloader.mods.upgradetree.Upgrade;
import com.garward.wurmmodloader.mods.upgradetree.UpgradeTreeManager;
import com.wurmonline.server.creatures.Creature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * declarativeui-driven Upgrade Tree window. Renders the tree as a viewport
 * with tier-row layout: blips coloured by status, edges from prerequisite to
 * dependent, click target buttons under each node.
 *
 * <p>Used only when the player has the client modloader and the
 * {@code com.garward.ui} channel active — {@link UpgradeTreeWindow} stays the
 * fallback for vanilla clients.
 */
public class UpgradeTreeWindowDeclarative {

    public static final String WINDOW_ID = "upgradetree";
    private static final int TREE_W = 820;
    private static final int TREE_H = 640;
    private static final int ROW_TOP = 80;
    private static final int ROW_STEP = 130;
    private static final int BLIP_DIAM = 22;

    private final Creature player;

    public UpgradeTreeWindowDeclarative(Creature player) {
        this.player = player;
    }

    public void show() {
        UpgradeTreeManager mgr = UpgradeTreeManager.getInstance();
        Set<String> unlocked = mgr.getPlayerUpgrades(player.getWurmId());
        int power = queryPower();

        List<Upgrade> all = new ArrayList<>(mgr.getAllUpgrades());
        all.sort(Comparator.comparingInt(Upgrade::getTier).thenComparing(Upgrade::getId));

        Map<Integer, List<Upgrade>> byTier = new TreeMap<>();
        for (Upgrade u : all) {
            byTier.computeIfAbsent(u.getTier(), k -> new ArrayList<>()).add(u);
        }

        Map<String, int[]> coords = new HashMap<>();
        int rowIdx = 0;
        for (Map.Entry<Integer, List<Upgrade>> e : byTier.entrySet()) {
            List<Upgrade> tierList = e.getValue();
            int n = tierList.size();
            int span = TREE_W - 80;
            int spacing = span / (n + 1);
            int y = ROW_TOP + rowIdx * ROW_STEP;
            for (int i = 0; i < n; i++) {
                coords.put(tierList.get(i).getId(), new int[]{40 + spacing * (i + 1), y});
            }
            rowIdx++;
        }

        List<WidgetNode> children = new ArrayList<>();

        for (Upgrade u : all) {
            int[] dst = coords.get(u.getId());
            for (String reqId : u.getRequirements()) {
                int[] src = coords.get(reqId);
                if (src == null) continue;
                boolean lit = unlocked.contains(reqId);
                String c = lit ? "0.5,0.9,0.5,0.85" : "0.55,0.55,0.65,0.5";
                children.add(Widgets.edge(src[0], src[1], dst[0], dst[1], 3, c));
            }
        }

        for (Upgrade u : all) {
            int[] xy = coords.get(u.getId());
            boolean isUnlocked = unlocked.contains(u.getId());
            boolean canUnlock = !isUnlocked && mgr.meetsRequirements(player.getWurmId(), u);

            String fill;
            String outline = "0.05,0.05,0.1,1.0";
            String status;
            if (isUnlocked) { fill = "0.40,0.90,0.40,1.0"; status = "UNLOCKED"; }
            else if (canUnlock) { fill = "0.95,0.85,0.35,1.0"; status = "AVAILABLE"; }
            else { fill = "0.45,0.45,0.50,0.75"; outline = "0.10,0.10,0.15,0.6"; status = "LOCKED"; }

            StringBuilder tip = new StringBuilder();
            tip.append(u.getName()).append('\n').append(u.getDescription())
               .append('\n').append("Cost: ").append(u.getCost()).append(" power")
               .append(" | Tier ").append(u.getTier())
               .append('\n').append("Status: ").append(status);
            if (u.hasRequirements()) {
                tip.append('\n').append("Requires: ");
                if (!u.getRequirements().isEmpty()) {
                    tip.append(String.join(", ", u.getRequirements()));
                }
                if (u.getMinUpgradesRequired() > 0) {
                    if (!u.getRequirements().isEmpty()) tip.append(" + ");
                    tip.append(u.getMinUpgradesRequired()).append(" total");
                }
            }
            String tipStr = tip.toString();

            children.add(Widgets.at(xy[0] - BLIP_DIAM / 2, xy[1] - BLIP_DIAM / 2,
                Widgets.blip(BLIP_DIAM, fill, 2, outline)).tooltip(tipStr));

            String action = isUnlocked ? "info:" + u.getId()
                          : (canUnlock ? "unlock:" + u.getId() : "info:" + u.getId());
            children.add(Widgets.at(xy[0] - 60, xy[1] + 16, 120, 22,
                Widgets.button(u.getName(), action, tipStr)));
        }

        children.add(Widgets.at(20, 12, 700, 22, Widgets.bindLabel("header",
            "Upgrade Tree — Power: " + power
                + "   Unlocked: " + unlocked.size() + " / " + all.size())));

        int legendY = TREE_H - 96;
        children.add(Widgets.at(20, legendY, 14, 14,
            Widgets.blip(14, "0.40,0.90,0.40,1.0", 1, "0,0,0,1")));
        children.add(Widgets.at(38, legendY - 2, 90, 18, Widgets.label("Unlocked")));
        children.add(Widgets.at(130, legendY, 14, 14,
            Widgets.blip(14, "0.95,0.85,0.35,1.0", 1, "0,0,0,1")));
        children.add(Widgets.at(148, legendY - 2, 90, 18, Widgets.label("Available")));
        children.add(Widgets.at(240, legendY, 14, 14,
            Widgets.blip(14, "0.45,0.45,0.50,0.75", 1, "0,0,0,0.6")));
        children.add(Widgets.at(258, legendY - 2, 90, 18, Widgets.label("Locked")));

        children.add(Widgets.at(TREE_W - 100, TREE_H - 40, 80, 26,
            Widgets.button("Close", "tree:close", "Dismiss this window")));

        WidgetNode viewport = Widgets.viewport(TREE_W, TREE_H,
            children.toArray(new WidgetNode[0]));
        Widgets.zoomBounds(viewport, 0.5, 2.5, 1.15);
        Widgets.zoomAnchor(viewport, "center");

        ModActionEvent open = new ModActionEvent("ui:open_window");
        open.set("player", player);
        open.set("windowId", WINDOW_ID);
        open.set("title", "Upgrade Tree");
        open.set("width", TREE_W);
        open.set("height", TREE_H);
        open.set("tree", viewport);
        EventBus.getInstance().post(open);
    }

    public void close() {
        ModActionEvent ev = new ModActionEvent("ui:close_window");
        ev.set("player", player);
        ev.set("windowId", WINDOW_ID);
        EventBus.getInstance().post(ev);
    }

    private int queryPower() {
        ModQueryEvent q = new ModQueryEvent("powerscaling:power_level");
        q.set("playerWurmId", player.getWurmId());
        EventBus.getInstance().post(q);
        return q.isHandled() ? q.getInt("powerLevel") : 0;
    }
}
