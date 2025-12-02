package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class MuteManager {

    private static FileConfiguration config;
    private static File file;

    private MuteManager() {}

    public static void init(HardcorePlugin plugin) {
        file = new File(plugin.getDataFolder(), "mutes.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            config = new YamlConfiguration();
            save();
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static boolean isMuted(UUID uuid) {
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
        return config.getString(path(uuid, "reason"), "Muted by an administrator.");
    }

    public static void mute(OfflinePlayer target, String reason, Duration duration, String moderator) {
        long until = duration == null ? -1L : System.currentTimeMillis() + duration.toMillis();

        config.set(path(target.getUniqueId(), "name"), target.getName());
        config.set(path(target.getUniqueId(), "reason"), reason);
        config.set(path(target.getUniqueId(), "until"), until);
        config.set(path(target.getUniqueId(), "by"), moderator);
        save();
    }

    public static boolean unmute(UUID uuid) {
        if (!config.contains(path(uuid))) {
            return false;
        }

        config.set(path(uuid), null);
        save();
        return true;
    }

    public static void save() {
        if (config == null || file == null) {
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save mutes.yml: " + e.getMessage());
        }
    }

    private static String path(UUID uuid) {
        return "mutes." + uuid;
    }

    private static String path(UUID uuid, String child) {
        return path(uuid) + "." + child;
    }
}
