package com.garward.wurmmodloader.serverlauncher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Mirrors System.out/System.err to a log file while preserving the original console.
 */
final class ConsoleTee {

    private static volatile boolean installed;

    private ConsoleTee() {}

    static void install(Path targetFile) throws IOException {
        if (installed) {
            return;
        }
        synchronized (ConsoleTee.class) {
            if (installed) {
                return;
            }

            Files.createDirectories(targetFile.getParent());
            OutputStream fileStream = Files.newOutputStream(
                targetFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            );
            OutputStream sharedStream = new SharedOutputStream(fileStream);

            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            PrintStream teeOut = new PrintStream(new TeeOutputStream(originalOut, sharedStream), true, StandardCharsets.UTF_8.name());
            PrintStream teeErr = new PrintStream(new TeeOutputStream(originalErr, sharedStream), true, StandardCharsets.UTF_8.name());

            System.setOut(teeOut);
            System.setErr(teeErr);

            installed = true;
        }
    }

    private static final class SharedOutputStream extends OutputStream {
        private final OutputStream delegate;

        private SharedOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public synchronized void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.flush();
        }
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream primary;
        private final OutputStream secondary;

        private TeeOutputStream(OutputStream primary, OutputStream secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            primary.write(b);
            secondary.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            primary.write(b, off, len);
            secondary.write(b, off, len);
        }

        @Override
        public synchronized void flush() throws IOException {
            primary.flush();
            secondary.flush();
        }

        @Override
        public void close() throws IOException {
            primary.flush();
            secondary.flush();
        }
    }
}
