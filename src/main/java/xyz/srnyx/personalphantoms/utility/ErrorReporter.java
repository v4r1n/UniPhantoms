package xyz.srnyx.personalphantoms.utility;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;


/**
 * Centralized error reporting and logging system
 */
public class ErrorReporter {
    @NotNull private final Logger logger;
    @NotNull private final File errorDir;
    private final boolean saveToFile;

    public ErrorReporter(@NotNull Logger logger, @NotNull File dataFolder, boolean saveToFile) {
        this.logger = logger;
        this.errorDir = new File(dataFolder, "errors");
        this.saveToFile = saveToFile;

        if (saveToFile && !errorDir.exists()) {
            errorDir.mkdirs();
        }
    }

    /**
     * Report an error with context
     *
     * @param context what was happening when the error occurred
     * @param throwable the exception
     */
    public void report(@NotNull String context, @NotNull Throwable throwable) {
        report(context, throwable, null);
    }

    /**
     * Report an error with context and additional info
     *
     * @param context what was happening
     * @param throwable the exception
     * @param additionalInfo extra debug information
     */
    public void report(@NotNull String context, @NotNull Throwable throwable, @Nullable String additionalInfo) {
        // Log to console
        logger.severe("========== ERROR REPORT ==========");
        logger.severe("Context: " + context);
        if (additionalInfo != null) {
            logger.severe("Info: " + additionalInfo);
        }
        logger.severe("Exception: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        logger.severe("Stack Trace:");

        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        final String stackTrace = sw.toString();

        for (final String line : stackTrace.split("\n")) {
            logger.severe("  " + line);
        }
        logger.severe("==================================");

        // Save to file if enabled
        if (saveToFile) {
            saveErrorToFile(context, throwable, additionalInfo, stackTrace);
        }
    }

    /**
     * Report a warning (non-critical error)
     *
     * @param context what was happening
     * @param message the warning message
     */
    public void warn(@NotNull String context, @NotNull String message) {
        logger.warning("[" + context + "] " + message);
    }

    /**
     * Report info for debugging
     *
     * @param context what's happening
     * @param message the info message
     */
    public void info(@NotNull String context, @NotNull String message) {
        logger.info("[" + context + "] " + message);
    }

    /**
     * Save error details to a file
     */
    private void saveErrorToFile(@NotNull String context, @NotNull Throwable throwable, @Nullable String additionalInfo, @NotNull String stackTrace) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        final String filename = "error_" + sdf.format(new Date()) + ".txt";
        final File errorFile = new File(errorDir, filename);

        try (final FileWriter writer = new FileWriter(errorFile)) {
            writer.write("========== ERROR REPORT ==========\n");
            writer.write("Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("Context: " + context + "\n");

            if (additionalInfo != null) {
                writer.write("Additional Info: " + additionalInfo + "\n");
            }

            writer.write("\nException:\n");
            writer.write(throwable.getClass().getName() + ": " + throwable.getMessage() + "\n\n");

            writer.write("Stack Trace:\n");
            writer.write(stackTrace);

            writer.write("\n==================================\n");

            logger.info("Error report saved to: " + errorFile.getName());
        } catch (final IOException e) {
            logger.severe("Failed to save error report: " + e.getMessage());
        }
    }

    /**
     * Format exception for compact logging
     *
     * @param throwable the exception
     * @return formatted string
     */
    @NotNull
    public static String format(@NotNull Throwable throwable) {
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }

    /**
     * Get short stack trace (first 5 lines)
     *
     * @param throwable the exception
     * @return short stack trace
     */
    @NotNull
    public static String getShortStackTrace(@NotNull Throwable throwable) {
        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        final String[] lines = sw.toString().split("\n");

        final StringBuilder result = new StringBuilder();
        final int limit = Math.min(5, lines.length);

        for (int i = 0; i < limit; i++) {
            result.append(lines[i]);
            if (i < limit - 1) result.append("\n");
        }

        if (lines.length > 5) {
            result.append("\n... (").append(lines.length - 5).append(" more lines)");
        }

        return result.toString();
    }
}
