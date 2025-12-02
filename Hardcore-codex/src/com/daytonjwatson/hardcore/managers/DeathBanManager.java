package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class DeathBanManager {

    private static final String DEATH_BANS_NODE = "deathbans";

    private static FileConfiguration config;
    private static File file;

    private DeathBanManager() {}

    public static void init(HardcorePlugin plugin) {
        file = new File(plugin.getDataFolder(), "deathban.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            config = new YamlConfiguration();
            config.createSection(DEATH_BANS_NODE);
            save();
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection(DEATH_BANS_NODE)) {
            config.createSection(DEATH_BANS_NODE);
            save();
        }
    }

    public static void recordDeathBan(OfflinePlayer player, String reason, String killer) {
        UUID uuid = player.getUniqueId();
        config.set(path(uuid, "name"), player.getName());
        config.set(path(uuid, "reason"), reason);
        config.set(path(uuid, "killer"), killer);
        config.set(path(uuid, "time"), System.currentTimeMillis());
        save();
    }

    public static void save() {
        if (config == null || file == null) {
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save deathban.yml: " + e.getMessage());
        }
    }

    private static String path(UUID uuid, String child) {
        return DEATH_BANS_NODE + "." + uuid + "." + child;
    }
}
