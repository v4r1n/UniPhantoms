package xyz.srnyx.uniphantoms.utility;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;


/**
 * Advanced time formatting utility
 */
public class TimeFormatter {

    /**
     * Format milliseconds to human-readable string
     *
     * @param milliseconds time in milliseconds
     * @return formatted string (e.g., "1 hour 30 minutes 45 seconds")
     */
    @NotNull
    public static String format(long milliseconds) {
        return format(milliseconds, TimeFormat.LONG);
    }

    /**
     * Format milliseconds with specified format
     *
     * @param milliseconds time in milliseconds
     * @param format the format style
     * @return formatted string
     */
    @NotNull
    public static String format(long milliseconds, @NotNull TimeFormat format) {
        if (milliseconds < 0) return "0s";

        final long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        milliseconds -= TimeUnit.DAYS.toMillis(days);

        final long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        milliseconds -= TimeUnit.HOURS.toMillis(hours);

        final long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes);

        final long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);

        final StringBuilder result = new StringBuilder();
        final String separator = format == TimeFormat.COMPACT ? "" : " ";

        if (days > 0) appendUnit(result, days, "day", format, separator);
        if (hours > 0) appendUnit(result, hours, "hour", format, separator);
        if (minutes > 0) appendUnit(result, minutes, "minute", format, separator);
        if (seconds > 0 || result.length() == 0) appendUnit(result, seconds, "second", format, separator);

        return result.toString().trim();
    }

    private static void appendUnit(@NotNull StringBuilder sb, long value, @NotNull String unit, @NotNull TimeFormat format, @NotNull String separator) {
        if (sb.length() > 0) sb.append(separator);
        sb.append(value);
        switch (format) {
            case LONG:
                sb.append(" ").append(unit);
                if (value != 1) sb.append("s");
                break;
            case SHORT:
            case COMPACT:
                sb.append(unit.charAt(0));
                break;
        }
    }

    /**
     * Format to relative time (e.g., "2 hours ago")
     *
     * @param timestamp the past timestamp in milliseconds
     * @return relative time string
     */
    @NotNull
    public static String formatRelative(long timestamp) {
        final long now = System.currentTimeMillis();
        final long diff = now - timestamp;

        if (diff < 0) return "in the future";
        if (diff < 1000) return "just now";

        final long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        final long hours = TimeUnit.MILLISECONDS.toHours(diff);
        final long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (days > 0) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }
        if (hours > 0) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        if (minutes > 0) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }
        return seconds == 1 ? "1 second ago" : seconds + " seconds ago";
    }

    /**
     * Parse time string to milliseconds
     * Supports formats: "1d2h3m4s", "1h30m", "45s", "30" (bare number = seconds), etc.
     *
     * @param timeString the time string to parse
     * @return milliseconds, or 0 if invalid
     */
    public static long parse(@NotNull String timeString) {
        long total = 0;
        final StringBuilder num = new StringBuilder();

        for (final char c : timeString.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (num.length() > 0) {
                final long value = Long.parseLong(num.toString());
                num.setLength(0);

                switch (Character.toLowerCase(c)) {
                    case 'd':
                        total += TimeUnit.DAYS.toMillis(value);
                        break;
                    case 'h':
                        total += TimeUnit.HOURS.toMillis(value);
                        break;
                    case 'm':
                        total += TimeUnit.MINUTES.toMillis(value);
                        break;
                    case 's':
                        total += TimeUnit.SECONDS.toMillis(value);
                        break;
                }
            }
        }

        // Handle trailing bare number (treat as seconds)
        if (num.length() > 0) {
            total += TimeUnit.SECONDS.toMillis(Long.parseLong(num.toString()));
        }

        return total;
    }

    /**
     * Time format styles
     */
    public enum TimeFormat {
        /**
         * Long format: "1 day", "2 hours", "30 minutes", "45 seconds"
         */
        LONG,

        /**
         * Short format: "1d 2h 30m 45s"
         */
        SHORT,

        /**
         * Compact format: "1d2h30m45s" (no spaces)
         */
        COMPACT
    }
}
