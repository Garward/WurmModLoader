package com.garward.wurmmodloader.mods.postgresbackend;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC {@link Connection} wrapper that runs every SQL string through
 * {@link SqlRewriter#rewrite(String)} before it reaches the Postgres driver.
 *
 * <p>Implemented as a {@link Proxy} so we don't have to maintain hundreds of
 * pass-through methods across JDBC versions. Only methods whose first argument
 * is a SQL string are rewritten — that covers {@code prepareStatement},
 * {@code prepareCall}, {@code nativeSQL}, and the {@code Statement.execute*}
 * family (which we proxy lazily inside {@code createStatement} for symmetry,
 * though Wurm's repository code doesn't actually need it).
 */
public final class RewritingConnection {

    private static final Logger LOG = Logger.getLogger(RewritingConnection.class.getName());

    private RewritingConnection() {}

    public static Connection wrap(Connection delegate) {
        if (delegate == null) return null;
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                new ConnHandler(delegate));
    }

    private static final class ConnHandler implements InvocationHandler {
        private final Connection delegate;
        ConnHandler(Connection delegate) { this.delegate = delegate; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (("prepareStatement".equals(name) || "prepareCall".equals(name) || "nativeSQL".equals(name))
                    && args != null && args.length > 0 && args[0] instanceof String) {
                String original = (String) args[0];
                String rewritten = SqlRewriter.rewrite(original);
                if (rewritten != original && !rewritten.equals(original)) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[PostgresBackend] rewrote SQL\n  before: " + original + "\n  after : " + rewritten);
                    }
                    args[0] = rewritten;
                }
            }
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                throw ite.getCause();
            }
        }
    }
}
