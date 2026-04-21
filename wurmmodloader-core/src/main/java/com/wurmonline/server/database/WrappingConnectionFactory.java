package com.wurmonline.server.database;

import com.garward.wurmmodloader.api.database.Dialect;
import com.garward.wurmmodloader.api.database.SqlDialectRewriter;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Framework-owned {@link ConnectionFactory} that wraps a mod-supplied factory.
 *
 * <p>Lives in {@code com.wurmonline.server.database} so it can reach the
 * package-private {@code ConnectionFactory(String, WurmDatabaseSchema)}
 * constructor without reflection. The rest of the framework is unaffected —
 * this class is only instantiated by
 * {@code com.garward.wurmmodloader.core.eventlogic.database.DatabaseBackendEventLogic}
 * when a custom {@code DatabaseBackend} takes over.</p>
 *
 * <p>Responsibilities on {@link #createConnection()}:</p>
 * <ol>
 *   <li>Delegate to the mod's factory.</li>
 *   <li>Optionally wrap the returned {@link Connection} through
 *       {@link SqlDialectRewriter#wrap(Connection, Dialect)} so vanilla-WU
 *       SQLite-isms translate on the fly. {@link Dialect#CUSTOM} and
 *       {@link Dialect#SQLITE} skip wrapping.</li>
 *   <li>Fire {@link com.garward.wurmmodloader.api.events.database.DatabaseConnectionOpenedEvent}
 *       via {@link ProxyServerHook#fireDatabaseConnectionOpenedEvent(WurmDatabaseSchema, Connection)}
 *       so subscribers can apply session pragmas regardless of which backend
 *       is active.</li>
 * </ol>
 *
 * <p>{@link #isValid(Connection)} and {@link #isStale(long, Connection)}
 * delegate to the mod's factory verbatim; the wrapper does not second-guess
 * pool / health semantics.</p>
 */
public final class WrappingConnectionFactory extends ConnectionFactory {

    private final ConnectionFactory delegate;
    private final Dialect dialect;

    public WrappingConnectionFactory(ConnectionFactory delegate, Dialect dialect) {
        super(delegate.getUrl(), delegate.getSchema());
        this.delegate = delegate;
        this.dialect = dialect == null ? Dialect.CUSTOM : dialect;
    }

    @Override
    public Connection createConnection() throws SQLException {
        Connection raw = delegate.createConnection();
        Connection out = SqlDialectRewriter.wrap(raw, dialect);
        try {
            ProxyServerHook.fireDatabaseConnectionOpenedEvent(getSchema(), out);
        } catch (Throwable t) {
            // Event handlers must never break connection acquisition.
            // The proxy hook already logs internally; rethrow only SQLExceptions.
            if (t instanceof SQLException) throw (SQLException) t;
        }
        return out;
    }

    @Override
    public boolean isValid(Connection conn) throws SQLException {
        return delegate.isValid(conn);
    }

    @Override
    public boolean isStale(long age, Connection conn) throws SQLException {
        return delegate.isStale(age, conn);
    }

    public ConnectionFactory getDelegate() {
        return delegate;
    }

    public Dialect getDialect() {
        return dialect;
    }
}
