package com.garward.wurmmodloader.core.console;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.player.PlayerMessageEvent;
import com.garward.wurmmodloader.core.event.EventBus;

import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Bridges the framework's custom GM commands (the ones added on top of
 * vanilla — see {@link ConsoleGMCommandRouter#dispatchCustomWithSink}) into
 * the in-game chat dispatcher.
 *
 * <p>Subscribes to {@link PlayerMessageEvent} so any {@code #}-prefixed message
 * typed by a GM character can be intercepted before vanilla's
 * {@code Communicator.reallyHandle_CMD_MESSAGE} switch runs. When the command
 * matches one of ours, the handler runs on the calling thread with output piped
 * to the player's chat via {@link Communicator#sendNormalServerMessage(String)}
 * and the event is cancelled so vanilla doesn't reply "Unknown command".
 *
 * <p>{@code #help} with no args goes through vanilla — {@code GmHelpAddendumPatch}
 * appends our addendum after vanilla's help finishes. {@code #help <query>} is
 * intercepted here and runs a substring search over the
 * {@link GmHelpIndex} (vanilla + framework lines, gated by caller power).
 */
public final class InGameGMCommandBridge {

    private static final Logger LOGGER = Logger.getLogger(InGameGMCommandBridge.class.getName());
    /** Minimum power level required to use any framework GM command in-game. */
    private static final int MIN_POWER = 2;

    /**
     * Custom commands the bridge takes over in-game. Commands that exist in
     * vanilla (#kick, #ban, #setpower, #summon, #who, #toggleglobal, #shutdown)
     * are deliberately omitted — vanilla owns them. We only route framework
     * additions, plus #send (vanilla exists but we extended it for tile coords).
     */
    private static final Set<String> ROUTED = new HashSet<>(Arrays.asList(
        "send",
        "listall",
        "serverinfo",
        "findplayer",
        "createitem",
        "giveskill",
        "spawncreature",
        "weather",
        "time",
        "sendmessage",
        "villageperm",
        "setmayor",
        "villageguards",
        "movealtar"
    ));

    /** Framework help lines (single source of truth for both addendum print + index). */
    private static final List<String> FRAMEWORK_LINES = Arrays.asList(
        "[Teleport / positioning]",
        "  #send <player> <tx> <ty>         - teleport using in-game tile coords (F1 readout)",
        "  #send <player> meter <x> <y>     - teleport using raw meter coords",
        "[Item / skill / creature]",
        "  #createitem <name-or-id> [ql] [amount]   - fuzzy name match OK (e.g. 'longsword')",
        "  #giveskill <player> <skill> <value>      - fuzzy skill-name match",
        "  #spawncreature <name> [count] [tx] [ty]  - fuzzy creature-name match",
        "[Players / server]",
        "  #listall                         - list every online player across all kingdoms",
        "  #who                             - short online-player listing",
        "  #findplayer <partial>            - locate a player by partial name",
        "  #serverinfo                      - uptime / tick / world info",
        "  #sendmessage <player> <message>  - DM a specific player from the server",
        "  #shutdown <minutes> <reason>     - safe shutdown w/ DB flush (use this, not Ctrl-C)",
        "  #time [day|night|dawn|dusk|HH:MM]- set server time of day",
        "  #weather [clear|rain|storm|...]  - set weather",
        "[Villages / deeds]",
        "  #villageperm <deed> <role> <perm> <on|off>",
        "     role: everybody | mayor | citizen | ally",
        "     perm: Build, PassGates, MineIron, ManageRoles, ...",
        "  #setmayor <deed> <playername>    - player must be online",
        "  #villageguards <deed> <count>    - works on permanent / GM deeds",
        "[Altars / fixtures]",
        "  #movealtar <holy|unholy> player <name>",
        "  #movealtar <holy|unholy> tile <tx> <ty>"
    );

    private static volatile boolean registered = false;

    private InGameGMCommandBridge() {}

    /** Idempotent. Called from framework startup alongside the console router. */
    public static synchronized void install() {
        if (registered) return;
        EventBus.getInstance().register(new InGameGMCommandBridge());
        // Register framework lines into the searchable index. All require GM power.
        for (String line : FRAMEWORK_LINES) {
            GmHelpIndex.addFramework(line, MIN_POWER);
        }
        registered = true;
        LOGGER.info("[InGameGM] Framework custom commands registered for in-game GM use.");
    }

    @SubscribeEvent
    public void onPlayerMessage(PlayerMessageEvent event) {
        String message = event.getMessage();
        if (message == null || message.isEmpty() || message.charAt(0) != '#') return;

        Object commObj = event.getCommunicator();
        if (!(commObj instanceof Communicator)) return;
        Communicator comm = (Communicator) commObj;

        Player player;
        try {
            player = comm.getPlayer();
        } catch (Throwable t) {
            return;
        }
        if (player == null) return;
        int power = player.getPower();

        String body = message.substring(1).trim();
        if (body.isEmpty()) return;

        String[] parts = body.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        // #help <query> → fuzzy-search the index. Bare #help falls through to vanilla.
        if ("help".equals(cmd) && !args.isEmpty()) {
            if (power < 1) return;
            handleHelpQuery(comm, args, power);
            event.setCancelled(true);
            return;
        }

        if (!ROUTED.contains(cmd)) return;
        if (power < MIN_POWER) return; // Insufficient power — let vanilla handle (it'll reject too).

        Consumer<String> sink = comm::sendNormalServerMessage;
        try {
            ConsoleGMCommandRouter.dispatchCustomWithSink(cmd, args, sink);
        } catch (Throwable t) {
            sink.accept("[GM] Command failed: " + t.getClass().getSimpleName()
                + ": " + t.getMessage());
            LOGGER.warning("[InGameGM] Handler threw on '" + cmd + "': " + t);
        }
        event.setCancelled(true);
    }

    private static void handleHelpQuery(Communicator comm, String query, int power) {
        Consumer<String> s = comm::sendNormalServerMessage;
        List<GmHelpIndex.Entry> hits = GmHelpIndex.search(query, power);
        if (hits.isEmpty()) {
            s.accept("No GM commands match \"" + query + "\".");
            return;
        }
        s.accept("=== #help \"" + query + "\" — " + hits.size() + " match"
            + (hits.size() == 1 ? "" : "es") + " ===");
        boolean inFrameworkSection = false;
        for (GmHelpIndex.Entry e : hits) {
            if (e.framework && !inFrameworkSection) {
                s.accept("--- framework additions ---");
                inFrameworkSection = true;
            }
            s.accept(e.line);
        }
    }

    /**
     * Called from {@code GmHelpAddendumPatch} after vanilla's
     * {@code handleHashMessageHelp} finishes. Lists the framework's custom GM
     * commands below vanilla's built-in help.
     */
    public static void appendHelpAddendum(Communicator comm, int power) {
        if (comm == null || power < MIN_POWER) return;
        Consumer<String> s = comm::sendNormalServerMessage;

        s.accept(" ");
        s.accept("========== Framework GM commands (WurmModLoader) ==========");
        for (String line : FRAMEWORK_LINES) {
            s.accept(line);
        }
        s.accept(" ");
        s.accept("Tip: '#help <query>' fuzzy-searches all GM commands");
        s.accept("     (vanilla + framework). Example: #help village");
        s.accept("===========================================================");
    }

    /** Exposed for tests/diagnostics. */
    public static List<String> frameworkLines() {
        return new ArrayList<>(FRAMEWORK_LINES);
    }
}
