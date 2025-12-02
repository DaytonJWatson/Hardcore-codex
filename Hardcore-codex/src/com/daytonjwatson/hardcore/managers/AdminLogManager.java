package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class AdminLogManager {

    private static final String LOG_NODE = "logs";
    private static final int MAX_LOG_ENTRIES = 500;

    private static FileConfiguration config;
    private static File file;

    private AdminLogManager() {}

    public static void init(HardcorePlugin plugin) {
        file = new File(plugin.getDataFolder(), "adminlog.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            config = new YamlConfiguration();
            config.set(LOG_NODE, new ArrayList<String>());
            save();
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isList(LOG_NODE)) {
            config.set(LOG_NODE, new ArrayList<String>());
            save();
        }
    }

    public static synchronized void log(CommandSender sender, String command, boolean allowed) {
        if (config == null) {
            return;
        }

        List<String> logs = new ArrayList<>(config.getStringList(LOG_NODE));
        String status = allowed ? "ALLOWED" : "DENIED";
        String actor = sender instanceof ConsoleCommandSender ? "Console" : sender.getName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        logs.add(timestamp + " [" + status + "] " + actor + ": " + command.trim());

        if (logs.size() > MAX_LOG_ENTRIES) {
            logs = logs.subList(logs.size() - MAX_LOG_ENTRIES, logs.size());
        }

        config.set(LOG_NODE, logs);
        save();
    }

    public static void save() {
        if (config == null || file == null) {
            return;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
