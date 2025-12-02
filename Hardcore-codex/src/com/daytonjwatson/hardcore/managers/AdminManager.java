package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class AdminManager {

    private static final String ADMINS_NODE = "admins";

    private static FileConfiguration config;
    private static File file;

    private AdminManager() {}

    public static void init(HardcorePlugin plugin) {
        file = new File(plugin.getDataFolder(), "admins.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            config = new YamlConfiguration();
            config.createSection(ADMINS_NODE);
            save();
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection(ADMINS_NODE)) {
            config.createSection(ADMINS_NODE);
            save();
        }
    }

    public static boolean hasAdmins() {
        ConfigurationSection section = config.getConfigurationSection(ADMINS_NODE);
        return section != null && !section.getKeys(false).isEmpty();
    }

    public static boolean isAdmin(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }

        if (sender instanceof org.bukkit.entity.Player player) {
            return isAdmin(player.getUniqueId());
        }

        return false;
    }

    public static boolean isAdmin(UUID uuid) {
        ConfigurationSection section = config.getConfigurationSection(ADMINS_NODE);
        if (section == null) {
            return false;
        }

        return section.getKeys(false).contains(uuid.toString());
    }

    public static boolean addAdmin(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (isAdmin(uuid)) {
            return false;
        }

        config.set(ADMINS_NODE + "." + uuid, player.getName());
        save();
        return true;
    }

    public static boolean removeAdmin(OfflinePlayer player) {
        return removeAdmin(player.getUniqueId());
    }

    public static boolean removeAdmin(UUID uuid) {
        if (!isAdmin(uuid)) {
            return false;
        }

        config.set(ADMINS_NODE + "." + uuid, null);
        save();
        return true;
    }

    public static List<String> getAdminLabels() {
        List<String> admins = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection(ADMINS_NODE);
        if (section == null) {
            return admins;
        }

        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            String name = section.getString(key, "Unknown");
            admins.add(name + " (" + key + ")");
        }

        return admins;
    }

    public static void save() {
        if (config == null || file == null) {
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save admins.yml: " + e.getMessage());
        }
    }
}
