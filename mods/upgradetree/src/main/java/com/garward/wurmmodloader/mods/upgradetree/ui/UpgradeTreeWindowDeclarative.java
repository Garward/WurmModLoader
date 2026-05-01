package com.garward.wurmmodloader.mods.upgradetree.ui;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.modsupport.declarativeui.WidgetNode;
import com.garward.wurmmodloader.modsupport.declarativeui.Widgets;
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
    private static final int NODE_DIAM = 36;
    private static final int NODE_BORDER = 3;
    private static final int HALO_DIAM = 72;
    private static final int TIER_LABEL_X = 8;
    private static final int TIER_LABEL_W = 60;
    // Grid lattice: every node lands at integer (col, row). Cell width = node
    // diameter so adjacent cells touch exactly; gap of N cells = N node-widths
    // of empty space. Cell height taller than NODE_DIAM gives breathing room
    // between tiers without breaking horizontal alignment.
    private static final int CELL_W = NODE_DIAM;
    private static final int CELL_H = NODE_DIAM * 3;

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
        int gridOriginX = TIER_LABEL_X + TIER_LABEL_W + 8;
        int contentSpan = TREE_W - gridOriginX - 20;
        // Total grid columns available. Every node lands at gridOriginX + col
        // * CELL_W for some integer col.
        int totalCols = Math.max(1, contentSpan / CELL_W);
        // Pick stepCells once, off the widest tier — every tier uses the same
        // step so columns line up vertically across rows. Without this, tier-2
        // (4 nodes) and tier-3 (5 nodes) snap to different lattices and the
        // tree looks scattered even though each row is internally on-grid.
        int maxN = 1;
        for (List<Upgrade> tierList : byTier.values()) {
            if (tierList.size() > maxN) maxN = tierList.size();
        }
        int stepCells = maxN > 1 ? Math.max(1, (totalCols - 1) / (maxN - 1)) : 0;
        int midCol = totalCols / 2;
        int rowIdx = 0;
        for (Map.Entry<Integer, List<Upgrade>> e : byTier.entrySet()) {
            List<Upgrade> tierList = e.getValue();
            int n = tierList.size();
            int y = ROW_TOP + rowIdx * CELL_H;
            // Place all rows centered on midCol with the same step. Even/odd
            // n both fall on a shared lattice (multiples of step from mid).
            int leftCol = midCol - (stepCells * (n - 1)) / 2;
            for (int i = 0; i < n; i++) {
                int col = leftCol + i * stepCells;
                coords.put(tierList.get(i).getId(),
                    new int[]{gridOriginX + col * CELL_W, y});
            }
            rowIdx++;
        }

        List<WidgetNode> children = new ArrayList<>();

        // Tier row labels on the left, aligned to each row's CELL_H step.
        int labelRow = 0;
        for (Map.Entry<Integer, List<Upgrade>> e : byTier.entrySet()) {
            int y = ROW_TOP + labelRow * CELL_H - 8;
            children.add(Widgets.at(TIER_LABEL_X, y, TIER_LABEL_W, 18,
                Widgets.label("Tier " + e.getKey())));
            labelRow++;
        }

        // Edges first so frames/halos overlap them. Lit edges (source already
        // unlocked) get a glow to draw the eye toward unlock paths. Endpoints
        // are pulled in by the node radius so the line meets the circle rim
        // rather than disappearing under the fill.
        int radius = NODE_DIAM / 2;
        for (Upgrade u : all) {
            int[] dst = coords.get(u.getId());
            for (String reqId : u.getRequirements()) {
                int[] src = coords.get(reqId);
                if (src == null) continue;
                double dx = dst[0] - src[0];
                double dy = dst[1] - src[1];
                double len = Math.hypot(dx, dy);
                if (len < radius * 2) continue;
                double ux = dx / len;
                double uy = dy / len;
                int x1 = src[0] + (int) Math.round(ux * radius);
                int y1 = src[1] + (int) Math.round(uy * radius);
                int x2 = dst[0] - (int) Math.round(ux * radius);
                int y2 = dst[1] - (int) Math.round(uy * radius);
                boolean lit = unlocked.contains(reqId);
                String c = lit ? "0.55,0.95,0.55,0.95" : "0.55,0.55,0.65,0.5";
                WidgetNode edge = Widgets.edge(x1, y1, x2, y2, 3, c);
                if (lit) Widgets.glowEdge(edge, 9, "0.40,0.90,0.40,0.35");
                children.add(edge);
            }
        }

        // Halos drawn before frames so they sit behind. Available nodes get a
        // bright halo to advertise pickability; unlocked gets a softer green
        // glow; locked gets nothing.
        for (Upgrade u : all) {
            int[] xy = coords.get(u.getId());
            boolean isUnlocked = unlocked.contains(u.getId());
            boolean canUnlock = !isUnlocked && mgr.meetsRequirements(player.getWurmId(), u);
            if (canUnlock) {
                children.add(Widgets.at(xy[0], xy[1],
                    Widgets.halo(HALO_DIAM, "1.00,0.85,0.30,0.85")));
            } else if (isUnlocked) {
                children.add(Widgets.at(xy[0], xy[1],
                    Widgets.halo(HALO_DIAM - 12, "0.40,0.95,0.45,0.55")));
            }
        }

        for (Upgrade u : all) {
            int[] xy = coords.get(u.getId());
            boolean isUnlocked = unlocked.contains(u.getId());
            boolean canUnlock = !isUnlocked && mgr.meetsRequirements(player.getWurmId(), u);

            String border;
            String fill;
            String status;
            if (isUnlocked) {
                border = "0.55,1.00,0.55,1.0";
                fill = "0.10,0.30,0.12,0.85";
                status = "UNLOCKED";
            } else if (canUnlock) {
                border = "1.00,0.90,0.35,1.0";
                fill = "0.25,0.20,0.05,0.80";
                status = "AVAILABLE";
            } else {
                border = "0.45,0.45,0.55,0.85";
                fill = "0.10,0.10,0.14,0.70";
                status = "LOCKED";
            }

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

            String action = (canUnlock ? "unlock:" : "info:") + u.getId();
            // Frame is the click target; renderer centers it on (x, y).
            WidgetNode frame = Widgets.action(
                Widgets.frame("circle", NODE_DIAM, NODE_DIAM, NODE_BORDER, fill, border, null),
                action).tooltip(tipStr);
            children.add(Widgets.at(xy[0], xy[1], frame));
        }

        children.add(Widgets.at(20, 12, 700, 22, Widgets.bindLabel("header",
            "Upgrade Tree — Power: " + power
                + "   Unlocked: " + unlocked.size() + " / " + all.size())));

        int legendY = TREE_H - 96;
        children.add(Widgets.at(20, legendY, 16, 16, Widgets.frame("circle", 16, 16, 2,
            "0.10,0.30,0.12,0.85", "0.55,1.00,0.55,1.0", null)));
        children.add(Widgets.at(40, legendY - 2, 90, 18, Widgets.label("Unlocked")));
        children.add(Widgets.at(130, legendY, 16, 16, Widgets.frame("circle", 16, 16, 2,
            "0.25,0.20,0.05,0.80", "1.00,0.90,0.35,1.0", null)));
        children.add(Widgets.at(150, legendY - 2, 90, 18, Widgets.label("Available")));
        children.add(Widgets.at(240, legendY, 16, 16, Widgets.frame("circle", 16, 16, 2,
            "0.10,0.10,0.14,0.70", "0.45,0.45,0.55,0.85", null)));
        children.add(Widgets.at(260, legendY - 2, 90, 18, Widgets.label("Locked")));

        children.add(Widgets.at(TREE_W - 100, TREE_H - 40, 80, 26,
            Widgets.button("Close", "tree:close", "Dismiss this window")));

        WidgetNode viewport = Widgets.viewport(TREE_W, TREE_H, "space_bg.jpg",
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
