package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class BanManager {

    private static final String BANS_NODE = "bans";

    private static FileConfiguration config;
    private static File file;

    private BanManager() {}

    public static void init(HardcorePlugin plugin) {
        file = new File(plugin.getDataFolder(), "bans.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            config = new YamlConfiguration();
            config.createSection(BANS_NODE);
            save();
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection(BANS_NODE)) {
            config.createSection(BANS_NODE);
            save();
        }
    }

    public static boolean isBanned(UUID uuid) {
        if (!config.contains(path(uuid))) {
            return false;
        }

        long until = config.getLong(path(uuid, "until"), -1L);
        if (until > 0 && until < System.currentTimeMillis()) {
            config.set(path(uuid), null);
            save();
            return false;
        }

        return true;
    }

    public static long getRemainingMillis(UUID uuid) {
        if (!config.contains(path(uuid))) {
            return 0L;
        }

        long until = config.getLong(path(uuid, "until"), -1L);
        if (until <= 0) {
            return -1L;
        }

        return Math.max(0L, until - System.currentTimeMillis());
    }

    public static String getReason(UUID uuid) {
        return config.getString(path(uuid, "reason"), "Banned by an administrator.");
    }

    public static String getBanner(UUID uuid) {
        return config.getString(path(uuid, "by"), "Unknown");
    }

    public static void ban(OfflinePlayer target, String reason, Duration duration, String moderator) {
        long until = duration == null ? -1L : System.currentTimeMillis() + duration.toMillis();

        config.set(path(target.getUniqueId(), "name"), target.getName());
        config.set(path(target.getUniqueId(), "reason"), reason);
        config.set(path(target.getUniqueId(), "until"), until);
        config.set(path(target.getUniqueId(), "by"), moderator);
        save();
    }

    public static boolean unban(UUID uuid) {
        if (!config.contains(path(uuid))) {
            return false;
        }

        config.set(path(uuid), null);
        save();
        return true;
    }

    public static List<String> getBannedNames() {
        List<String> banned = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection(BANS_NODE);
        if (section == null) {
            return banned;
        }

        boolean changed = false;
        for (String key : section.getKeys(false)) {
            long until = section.getLong(key + ".until", -1L);
            if (until > 0 && until < System.currentTimeMillis()) {
                section.set(key, null);
                changed = true;
                continue;
            }

            String name = section.getString(key + ".name", "Unknown");
            banned.add(name);
        }

        if (changed) {
            save();
        }

        return banned;
    }

    public static void save() {
        if (config == null || file == null) {
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save bans.yml: " + e.getMessage());
        }
    }

    private static String path(UUID uuid) {
        return BANS_NODE + "." + uuid;
    }

    private static String path(UUID uuid, String child) {
        return path(uuid) + "." + child;
    }
}
