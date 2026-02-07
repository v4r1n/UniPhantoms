package xyz.srnyx.personalphantoms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.file.AnnoyingResource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ConfigYml {
    @NotNull private final AnnoyingResource config;

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
        config = new AnnoyingResource(plugin, "config.yml");

        def = config.getBoolean("default", true);
        commandCooldown = config.getLong("command-cooldown", 600) * 1000; // default: 10 minutes
        statisticTask = new StatisticTask();
        worldsBlacklist = new WorldsBlacklist();
        errorReporting = new ErrorReporting();
        debugMode = config.getBoolean("debug-mode", false);
    }

    public class StatisticTask {
        @Nullable public final Long delay;
        public final long period;

        public StatisticTask() {
            this.delay = config.getString("statistic-task.delay", "automatic").equals("automatic")
                    ? null
                    : config.getLong("statistic-task.delay");
            this.period = config.getLong("statistic-task.period", 24000); // default: 20 minutes (1 in-game day)
        }
    }

    public class WorldsBlacklist {
        @Nullable public final Set<String> list;
        public final boolean treatAsWhitelist;

        public WorldsBlacklist() {
            this.treatAsWhitelist = config.getBoolean("worlds-blacklist.treat-as-whitelist", false);
            final List<String> stringList = config.getStringList("worlds-blacklist.list");
            this.list = stringList.isEmpty() && !treatAsWhitelist ? null : new HashSet<>(stringList);
        }
    }

    public class ErrorReporting {
        public final boolean saveToFile;
        public final boolean verbose;

        public ErrorReporting() {
            this.saveToFile = config.getBoolean("error-reporting.save-to-file", true);
            this.verbose = config.getBoolean("error-reporting.verbose", false);
        }
    }

}
