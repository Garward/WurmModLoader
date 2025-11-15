package com.garward.wurmmodloader.modloader.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Structured logging for the modloader boot sequence.
 * Provides clear phase separation and success/failure tracking.
 */
public class BootLogger {

	private static final Logger LOGGER = Logger.getLogger("ModLoader.Boot");

	private static long bootStartTime = 0;
	private static long phaseStartTime = 0;

	/**
	 * Start the boot sequence timer
	 */
	public static void startBoot() {
		bootStartTime = System.currentTimeMillis();
		String separator = "============================================================";
		LOGGER.info(separator);
		LOGGER.info("MODLOADER BOOT SEQUENCE STARTING");
		LOGGER.info(separator);
	}

	/**
	 * Start a new boot phase
	 *
	 * @param phase Phase number
	 * @param totalPhases Total number of phases
	 * @param name Phase name
	 */
	public static void startPhase(int phase, int totalPhases, String name) {
		phaseStartTime = System.currentTimeMillis();
		LOGGER.info("");
		LOGGER.info(String.format("[BOOT] === Phase %d/%d: %s ===", phase, totalPhases, name));
	}

	/**
	 * Complete a phase successfully
	 *
	 * @param phase Phase number
	 * @param totalPhases Total number of phases
	 */
	public static void completePhase(int phase, int totalPhases) {
		long elapsed = System.currentTimeMillis() - phaseStartTime;
		LOGGER.info(String.format("[BOOT] ✅ Phase %d/%d complete (%dms)", phase, totalPhases, elapsed));
	}

	/**
	 * Complete a phase with warnings
	 *
	 * @param phase Phase number
	 * @param totalPhases Total number of phases
	 * @param warningCount Number of warnings
	 */
	public static void completePhaseWithWarnings(int phase, int totalPhases, int warningCount) {
		long elapsed = System.currentTimeMillis() - phaseStartTime;
		LOGGER.warning(String.format("[BOOT] ⚠️ Phase %d/%d complete with %d warning(s) (%dms)",
			phase, totalPhases, warningCount, elapsed));
	}

	/**
	 * Complete a phase with errors
	 *
	 * @param phase Phase number
	 * @param totalPhases Total number of phases
	 * @param errorCount Number of errors
	 */
	public static void completePhaseWithErrors(int phase, int totalPhases, int errorCount) {
		long elapsed = System.currentTimeMillis() - phaseStartTime;
		LOGGER.severe(String.format("[BOOT] ❌ Phase %d/%d complete with %d error(s) (%dms)",
			phase, totalPhases, errorCount, elapsed));
	}

	/**
	 * Log a phase statistic
	 *
	 * @param category Category name (e.g., "MODLOADER", "BYTECODE")
	 * @param message Status message
	 */
	public static void phaseStat(String category, String message) {
		LOGGER.info(String.format("[%s] %s", category, message));
	}

	/**
	 * Log a phase warning
	 *
	 * @param category Category name
	 * @param message Warning message
	 */
	public static void phaseWarning(String category, String message) {
		LOGGER.warning(String.format("[%s] ⚠️ %s", category, message));
	}

	/**
	 * Log a phase error
	 *
	 * @param category Category name
	 * @param message Error message
	 */
	public static void phaseError(String category, String message) {
		LOGGER.severe(String.format("[%s] ❌ %s", category, message));
	}

	/**
	 * Complete the boot sequence
	 *
	 * @param totalErrors Total errors across all phases
	 * @param totalWarnings Total warnings across all phases
	 */
	public static void completeBoot(int totalErrors, int totalWarnings) {
		long totalTime = System.currentTimeMillis() - bootStartTime;
		double seconds = totalTime / 1000.0;

		String separator = "============================================================";
		LOGGER.info("");
		LOGGER.info(separator);
		if (totalErrors > 0) {
			LOGGER.severe(String.format("[BOOT] ❌ Boot complete with %d error(s) and %d warning(s) in %.2fs",
				totalErrors, totalWarnings, seconds));
		} else if (totalWarnings > 0) {
			LOGGER.warning(String.format("[BOOT] ⚠️ Boot complete with %d warning(s) in %.2fs",
				totalWarnings, seconds));
		} else {
			LOGGER.info(String.format("[BOOT] ✅ All systems ready in %.2fs", seconds));
		}
		LOGGER.info(separator);
	}

	/**
	 * Tracker for phase statistics
	 */
	public static class PhaseTracker {
		private final String category;
		private final List<String> errors = new ArrayList<>();
		private final List<String> warnings = new ArrayList<>();
		private int successCount = 0;
		private int totalCount = 0;

		public PhaseTracker(String category) {
			this.category = category;
		}

		public void recordSuccess() {
			successCount++;
			totalCount++;
		}

		public void recordError(String error) {
			errors.add(error);
			totalCount++;
		}

		public void recordWarning(String warning) {
			warnings.add(warning);
		}

		public void logResults() {
			if (errors.isEmpty() && warnings.isEmpty()) {
				phaseStat(category, String.format("Completed %d/%d successfully", successCount, totalCount));
			} else if (!errors.isEmpty()) {
				phaseError(category, String.format("Completed %d/%d successfully (%d failed)",
					successCount, totalCount, errors.size()));
				for (String error : errors) {
					phaseError(category, "Failed: " + error);
				}
			}

			if (!warnings.isEmpty()) {
				for (String warning : warnings) {
					phaseWarning(category, warning);
				}
			}
		}

		public int getErrorCount() {
			return errors.size();
		}

		public int getWarningCount() {
			return warnings.size();
		}

		public int getSuccessCount() {
			return successCount;
		}

		public int getTotalCount() {
			return totalCount;
		}
	}
}
