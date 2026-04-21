package com.garward.wurmmodloader.api.database;

/**
 * SQL dialect a {@link DatabaseBackend} speaks. Drives {@link SqlDialectRewriter}
 * so hardcoded vanilla-WU SQL fragments (notably the 16 {@code INSERT OR IGNORE}
 * sites) can be translated on the way to the driver without patching each call
 * site individually.
 *
 * <p>{@link #CUSTOM} means "do not rewrite, my backend handles SQL as-is."
 * That is the default to keep the SPI minimal and non-surprising — backends
 * opt in to rewriting by returning a concrete dialect.</p>
 *
 * @since 1.0.0
 */
public enum Dialect {
    SQLITE,
    MYSQL,
    MARIADB,
    POSTGRES,
    /** No framework rewriting. Backend owns any translation itself. */
    CUSTOM
}
