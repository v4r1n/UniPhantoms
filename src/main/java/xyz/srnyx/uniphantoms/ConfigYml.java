package xyz.srnyx.uniphantoms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.file.AnnoyingResource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ConfigYml {
    /**
     * {@code default}
     */
    public final boolean def;
    public final long commandCooldown;
    @NotNull public final StatisticTask statisticTask;
    @NotNull public final WorldsBlacklist worldsBlacklist;
    @NotNull public final ErrorReporting errorReporting;
    public final boolean debugMode;

    public ConfigYml(@NotNull AnnoyingPlugin plugin) {
        final AnnoyingResource config = new AnnoyingResource(plugin, "config.yml");

        def = config.getBoolean("default", true);
        commandCooldown = config.getLong("command-cooldown", 600) * 1000; // default: 10 minutes
        statisticTask = new StatisticTask(config);
        worldsBlacklist = new WorldsBlacklist(config);
        errorReporting = new ErrorReporting(config);
        debugMode = config.getBoolean("debug-mode", false);
    }

    public static class StatisticTask {
        @Nullable public final Long delay;
        public final long period;

        public StatisticTask(@NotNull AnnoyingResource config) {
            this.delay = config.getString("statistic-task.delay", "automatic").equals("automatic")
                    ? null
                    : config.getLong("statistic-task.delay");
            this.period = config.getLong("statistic-task.period", 24000); // default: 20 minutes (1 in-game day)
        }
    }

    public static class WorldsBlacklist {
        @Nullable public final Set<String> list;
        public final boolean treatAsWhitelist;

        public WorldsBlacklist(@NotNull AnnoyingResource config) {
            this.treatAsWhitelist = config.getBoolean("worlds-blacklist.treat-as-whitelist", false);
            final List<String> stringList = config.getStringList("worlds-blacklist.list");
            this.list = stringList.isEmpty() && !treatAsWhitelist ? null : new HashSet<>(stringList);
        }
    }

    public static class ErrorReporting {
        public final boolean saveToFile;
        public final boolean verbose;

        public ErrorReporting(@NotNull AnnoyingResource config) {
            this.saveToFile = config.getBoolean("error-reporting.save-to-file", true);
            this.verbose = config.getBoolean("error-reporting.verbose", false);
        }
    }

}
