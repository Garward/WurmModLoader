package com.garward.wurmmodloader.api.events.database;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.database.WurmDatabaseSchema;

import java.sql.Connection;

/**
 * Fired by the framework immediately after a JDBC {@link Connection} is successfully opened by a
 * {@code ConnectionFactory}.
 *
 * <p><b>Fired when:</b> {@code SqliteConnectionFactory.createConnection()} or
 * {@code MysqlConnectionFactory.createConnection()} returns a valid connection. If a mod-registered
 * {@link com.garward.wurmmodloader.api.database.DatabaseBackend} is in use, its own factory is
 * responsible for firing this event — only vanilla factories are auto-patched by the framework.</p>
 *
 * <p><b>Use this to:</b> apply session-level configuration to new connections, e.g.
 * {@code SET search_path}, {@code PRAGMA}s, session variables, role switches, or connection-pool
 * warmup hooks.</p>
 *
 * <p>This event is <b>not cancellable</b>.</p>
 *
 * @since 1.0.0
 */
public class DatabaseConnectionOpenedEvent extends Event {

    private final WurmDatabaseSchema schema;
    private final Connection connection;

    public DatabaseConnectionOpenedEvent(WurmDatabaseSchema schema, Connection connection) {
        super(false);
        this.schema = schema;
        this.connection = connection;
    }

    /** @return the schema this connection is bound to */
    public WurmDatabaseSchema getSchema() {
        return schema;
    }

    /** @return the freshly opened JDBC connection */
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return "DatabaseConnectionOpenedEvent{schema=" + schema + ", connection=" + connection + "}";
    }
}
