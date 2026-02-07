package xyz.srnyx.personalphantoms.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;


/**
 * Handles config versioning and migrations
 */
public class ConfigVersion {
    private static final int CURRENT_VERSION = 3; // Increment when config structure changes

    @NotNull private final Plugin plugin;
    @NotNull private final Logger logger;
    @NotNull private final File configFile;
    @NotNull private final File backupDir;

    public ConfigVersion(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.backupDir = new File(plugin.getDataFolder(), "backups");

        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    /**
     * Check if config needs migration
     *
     * @param config the config to check
     * @return true if migration is needed
     */
    public boolean needsMigration(@NotNull FileConfiguration config) {
        final int version = config.getInt("config-version", 1);
        return version < CURRENT_VERSION;
    }

    /**
     * Migrate config to current version
     *
     * @param config the config to migrate
     * @return true if migration was successful
     */
    public boolean migrate(@NotNull FileConfiguration config) {
        final int version = config.getInt("config-version", 1);

        if (version >= CURRENT_VERSION) {
            return true; // Already up to date
        }

        logger.info("Migrating config from version " + version + " to " + CURRENT_VERSION);

        // Backup before migration
        if (!backup()) {
            logger.severe("Failed to create backup! Migration aborted.");
            return false;
        }

        // Perform migrations step by step
        int currentVersion = version;
        boolean success = true;

        if (currentVersion == 1) {
            success = migrateV1ToV2(config);
            if (success) currentVersion = 2;
        }

        if (currentVersion == 2) {
            success = migrateV2ToV3(config);
            if (success) {
                currentVersion = 3;
                // V2->V3 needs file replacement, so we skip the normal save
                return true;
            }
        }

        if (success) {
            config.set("config-version", CURRENT_VERSION);
            try {
                config.save(configFile);
                logger.info("Config migrated successfully to version " + CURRENT_VERSION);
                return true;
            } catch (final IOException e) {
                logger.severe("Failed to save migrated config: " + e.getMessage());
                return false;
            }
        }

        logger.severe("Config migration failed!");
        return false;
    }

    /**
     * Migrate from version 1 to version 2
     * Version 2 adds error reporting configuration
     */
    private boolean migrateV1ToV2(@NotNull FileConfiguration config) {
        logger.info("Applying migration: V1 -> V2 (Adding error reporting)");

        // Add error reporting if doesn't exist
        if (!config.contains("error-reporting")) {
            config.set("error-reporting.save-to-file", true);
            config.set("error-reporting.verbose", false);
        }

        return true;
    }

    /**
     * Migrate from version 2 to version 3
     * Version 3 removes database section (now uses AnnoyingAPI's storage.yml)
     * Due to Bukkit API limitations, we regenerate the entire config file
     */
    private boolean migrateV2ToV3(@NotNull FileConfiguration config) {
        logger.info("Applying migration: V2 -> V3 (Removing database section, using storage.yml)");

        // Due to Bukkit FileConfiguration bug with set(key, null),
        // we need to delete the old file and let the plugin create a new one
        logger.info("Config structure changed significantly - regenerating config.yml");
        logger.info("Your old config has been backed up. The plugin will create a fresh config file.");

        // Delete the old config file
        if (configFile.exists()) {
            if (!configFile.delete()) {
                logger.severe("Failed to delete old config file!");
                return false;
            }
            logger.info("Old config file deleted successfully");
        }

        // Copy default config from JAR
        try {
            plugin.saveResource("config.yml", false);
            logger.info("New config file created from default template");
            return true;
        } catch (final Exception e) {
            logger.severe("Failed to create new config file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a backup of the current config
     *
     * @return true if backup was successful
     */
    public boolean backup() {
        if (!configFile.exists()) {
            return true; // Nothing to backup
        }

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        final String backupName = "config_" + sdf.format(new Date()) + ".yml";
        final File backupFile = new File(backupDir, backupName);

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Config backup created: " + backupName);
            return true;
        } catch (final IOException e) {
            logger.severe("Failed to create config backup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current config version number
     *
     * @return the current version
     */
    public static int getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
