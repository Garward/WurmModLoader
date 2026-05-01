package com.garward.wurmmodloader.core.serverpacks;

import com.garward.wurmmodloader.api.serverpacks.ServerPackOptions;

import java.nio.file.Path;

/**
 * Internal record for a registered pack — either path-backed or in-memory.
 * SHA-256 + size are filled lazily by {@link ServerPackHost} so the
 * canonical-channel announce manifest can carry delta-skip info.
 */
final class PackInfo {

    final Path path;
    final byte[] data;
    final boolean prepend;
    final boolean force;

    String sha256;
    long size;

    PackInfo(Path path, ServerPackOptions... options) {
        this.path = path;
        this.data = null;
        this.prepend = ServerPackOptions.PREPEND.isIn(options);
        this.force = ServerPackOptions.FORCE.isIn(options);
    }

    PackInfo(byte[] data, ServerPackOptions... options) {
        this.path = null;
        this.data = data;
        this.prepend = ServerPackOptions.PREPEND.isIn(options);
        this.force = ServerPackOptions.FORCE.isIn(options);
    }
}
