package com.garward.wurmmodloader.core.testing.eventsim;

import com.garward.wurmmodloader.api.events.base.Event;
import com.garward.wurmmodloader.core.testing.eventsim.EventIntegrityValidator.ValidationResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Telemetry logger for event simulation testing.
 *
 * <p>Tracks and logs:
 * <ul>
 *   <li>Event fire count per event type</li>
 *   <li>Validation success/failure rates</li>
 *   <li>Null parameter counts</li>
 *   <li>Exception details</li>
 *   <li>Timing information</li>
 * </ul>
 *
 * <p>Logs are written to: {@code logs/event_test.log}
 *
 * <p><strong>Thread Safety:</strong> Thread-safe using concurrent data structures.
 *
 * @since 1.0.0
 */
public class TelemetryLogger {

    private static final Logger LOGGER = Logger.getLogger(TelemetryLogger.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final Map<String, EventStats> eventStats = new ConcurrentHashMap<>();
    private final File logFile;
    private BufferedWriter logWriter;

    /**
     * Statistics for a single event type.
     */
    public static class EventStats {
        private final AtomicInteger fireCount = new AtomicInteger(0);
        private final AtomicInteger validCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);
        private final AtomicInteger nullCount = new AtomicInteger(0);
        private final AtomicInteger exceptionCount = new AtomicInteger(0);

        public void recordFired() {
            fireCount.incrementAndGet();
        }

        public void recordValidation(ValidationResult result) {
            if (result.isValid()) {
                validCount.incrementAndGet();
            } else {
                errorCount.incrementAndGet();
            }
            nullCount.addAndGet(result.getNullCount());
        }

        public void recordException() {
            exceptionCount.incrementAndGet();
        }

        public int getFireCount() {
            return fireCount.get();
        }

        public int getValidCount() {
            return validCount.get();
        }

        public int getErrorCount() {
            return errorCount.get();
        }

        public int getNullCount() {
            return nullCount.get();
        }

        public int getExceptionCount() {
            return exceptionCount.get();
        }

        public double getValidPercentage() {
            int total = fireCount.get();
            if (total == 0) return 0.0;
            return (validCount.get() * 100.0) / total;
        }
    }

    /**
     * Creates a new telemetry logger.
     *
     * @param logDirectory directory for log files (e.g., "logs")
     */
    public TelemetryLogger(String logDirectory) {
        this.logFile = new File(logDirectory, "event_test.log");
        initializeLogFile();
    }

    /**
     * Initializes the log file for writing.
     */
    private void initializeLogFile() {
        try {
            // Ensure directory exists
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Open log file in append mode
            logWriter = new BufferedWriter(new FileWriter(logFile, true));

            // Write header
            writeToLog("================================================================================");
            writeToLog("EVENT SIMULATION TEST SESSION STARTED");
            writeToLog("Timestamp: " + DATE_FORMAT.format(new Date()));
            writeToLog("================================================================================");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize event test log file", e);
        }
    }

    /**
     * Logs an event being fired.
     *
     * @param event the event being fired
     */
    public void logEventFired(Event event) {
        if (event == null) return;

        String eventName = event.getClass().getSimpleName();
        EventStats stats = eventStats.computeIfAbsent(eventName, k -> new EventStats());
        stats.recordFired();

        writeToLog(String.format("[FIRE] %s (#%d)", eventName, stats.getFireCount()));
    }

    /**
     * Logs validation results for an event.
     *
     * @param event the event that was validated
     * @param result the validation result
     */
    public void logValidation(Event event, ValidationResult result) {
        if (event == null || result == null) return;

        String eventName = event.getClass().getSimpleName();
        EventStats stats = eventStats.computeIfAbsent(eventName, k -> new EventStats());
        stats.recordValidation(result);

        if (!result.isValid()) {
            writeToLog(String.format("[VALIDATION FAILED] %s - Errors: %d, Warnings: %d, Nulls: %d",
                eventName, result.getErrors().size(), result.getWarnings().size(), result.getNullCount()));

            for (String error : result.getErrors()) {
                writeToLog("    ERROR: " + error);
            }
        }

        if (result.hasWarnings()) {
            for (String warning : result.getWarnings()) {
                writeToLog("    WARNING: " + warning);
            }
        }
    }

    /**
     * Logs an exception that occurred during event firing.
     *
     * @param event the event being fired
     * @param exception the exception that occurred
     */
    public void logException(Event event, Throwable exception) {
        if (event == null) return;

        String eventName = event.getClass().getSimpleName();
        EventStats stats = eventStats.computeIfAbsent(eventName, k -> new EventStats());
        stats.recordException();

        writeToLog(String.format("[EXCEPTION] %s - %s: %s",
            eventName,
            exception.getClass().getSimpleName(),
            exception.getMessage()));

        // Log stack trace
        for (StackTraceElement element : exception.getStackTrace()) {
            writeToLog("    at " + element.toString());
        }
    }

    /**
     * Logs a custom message.
     *
     * @param message the message to log
     */
    public void logMessage(String message) {
        writeToLog("[INFO] " + message);
    }

    /**
     * Writes a summary table of all event statistics.
     *
     * @return formatted summary table
     */
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("================================================================================\n");
        sb.append("EVENT SIMULATION TEST SUMMARY\n");
        sb.append("================================================================================\n");
        sb.append(String.format("%-40s %8s %8s %8s %8s %8s\n",
            "Event Name", "Fired", "Valid%", "Errors", "Nulls", "Except"));
        sb.append("--------------------------------------------------------------------------------\n");

        int totalFired = 0;
        int totalValid = 0;
        int totalErrors = 0;
        int totalNulls = 0;
        int totalExceptions = 0;

        for (Map.Entry<String, EventStats> entry : eventStats.entrySet()) {
            String eventName = entry.getKey();
            EventStats stats = entry.getValue();

            sb.append(String.format("%-40s %8d %7.1f%% %8d %8d %8d\n",
                truncate(eventName, 40),
                stats.getFireCount(),
                stats.getValidPercentage(),
                stats.getErrorCount(),
                stats.getNullCount(),
                stats.getExceptionCount()));

            totalFired += stats.getFireCount();
            totalValid += stats.getValidCount();
            totalErrors += stats.getErrorCount();
            totalNulls += stats.getNullCount();
            totalExceptions += stats.getExceptionCount();
        }

        sb.append("--------------------------------------------------------------------------------\n");
        double validPercent = totalFired > 0 ? (totalValid * 100.0 / totalFired) : 0.0;
        sb.append(String.format("%-40s %8d %7.1f%% %8d %8d %8d\n",
            "TOTAL", totalFired, validPercent, totalErrors, totalNulls, totalExceptions));
        sb.append("================================================================================\n");

        String summary = sb.toString();
        writeToLog(summary);
        return summary;
    }

    /**
     * Gets statistics for a specific event type.
     *
     * @param eventName the event class simple name
     * @return event statistics, or null if no data recorded
     */
    public EventStats getStats(String eventName) {
        return eventStats.get(eventName);
    }

    /**
     * Gets all event statistics.
     *
     * @return map of event name to statistics
     */
    public Map<String, EventStats> getAllStats() {
        return new ConcurrentHashMap<>(eventStats);
    }

    /**
     * Closes the log file and writes final summary.
     */
    public void close() {
        try {
            writeToLog("================================================================================");
            writeToLog("EVENT SIMULATION TEST SESSION ENDED");
            writeToLog("Timestamp: " + DATE_FORMAT.format(new Date()));
            writeToLog("================================================================================");
            writeToLog("");

            if (logWriter != null) {
                logWriter.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to close event test log", e);
        }
    }

    /**
     * Writes a line to the log file.
     *
     * @param message the message to write
     */
    private synchronized void writeToLog(String message) {
        try {
            if (logWriter != null) {
                logWriter.write(message);
                logWriter.newLine();
                logWriter.flush();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write to event test log", e);
        }
    }

    /**
     * Truncates a string to a maximum length.
     *
     * @param str the string to truncate
     * @param maxLength maximum length
     * @return truncated string
     */
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
