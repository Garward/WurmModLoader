package com.garward.wurmmodloader.examples.noopbackend;

import com.garward.wurmmodloader.api.database.DatabaseBackendRegistry;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.database.DatabaseBackendBootstrapEvent;
import com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent;
import com.garward.wurmmodloader.api.events.server.ServerStoppingEvent;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import java.util.logging.Logger;

/**
 * Reference example: a {@code DatabaseBackend} that logs every phase and
 * delegates to an embedded SQLite file per schema.
 *
 * <p>Purpose: let modders verify the full DatabaseBackend SPI wiring
 * (registration → selection → bootstrap → per-schema factories → connection
 * wrapping) end-to-end <b>without</b> standing up Postgres / MariaDB. The
 * backend really opens JDBC connections, so Wurm's boot completes and you can
 * observe the sequence in {@code wurmmodloader.0.log}.
 *
 * <p>Copy this folder, rename the classes, and replace the SQLite URL in
 * {@link com.wurmonline.server.database.NoOpConnectionFactory} with your own
 * driver URL (e.g. {@code jdbc:postgresql://...}). Flip
 * {@link NoOpDatabaseBackend#getDialect()} to match.
 *
 * @see NoOpDatabaseBackend
 */
public class NoOpBackendMod implements WurmServerMod {

    private static final Logger logger = Logger.getLogger(NoOpBackendMod.class.getName());

    @SubscribeEvent
    public void onSelect(DatabaseBackendSelectionEvent event) {
        logger.info("[NoOpBackend] DatabaseBackendSelectionEvent — registering NoOpDatabaseBackend");
        DatabaseBackendRegistry.register(new NoOpDatabaseBackend());
    }

    @SubscribeEvent
    public void onBootstrap(DatabaseBackendBootstrapEvent event) {
        logger.info("[NoOpBackend] DatabaseBackendBootstrapEvent fired — backend is "
            + event.getBackend().getName()
            + ", dialect=" + event.getBackend().getDialect());
        // A real Postgres backend would open an admin connection here and run
        // CREATE DATABASE IF NOT EXISTS for each WurmDatabaseSchema. The NoOp
        // backend uses per-schema SQLite files and has nothing to pre-create.
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Drain any pool / exporter thread your backend owns here, before WU
        // tears its own DbConnector connections down. The NoOp holds nothing
        // drainable — real HikariCP-backed Postgres backends would call
        // hikariDataSource.close() at this point.
        logger.info("[NoOpBackend] ServerStoppingEvent — no pool to drain (reference skeleton)");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
