package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class AdminLogManager {

    private static final String LOG_NODE = "logs";
    private static final int MAX_LOG_ENTRIES = 500;
    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter READABLE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        String timestamp = LocalDateTime.now().format(STORAGE_FORMAT);
        logs.add(timestamp + " [" + status + "] " + actor + ": " + command.trim());

        if (logs.size() > MAX_LOG_ENTRIES) {
            logs = logs.subList(logs.size() - MAX_LOG_ENTRIES, logs.size());
        }

        config.set(LOG_NODE, logs);
        save();
    }

    public static synchronized List<String> getRecentLogs(int limit, String actorFilter) {
        if (config == null) {
            return List.of();
        }

        List<String> logs = config.getStringList(LOG_NODE);
        List<String> results = new ArrayList<>();

        for (int i = logs.size() - 1; i >= 0 && results.size() < limit; i--) {
            String entry = logs.get(i);
            ParsedLog parsed = parse(entry);
            if (parsed == null) {
                continue;
            }

            if (actorFilter != null && !parsed.actor.equalsIgnoreCase(actorFilter)) {
                continue;
            }

            results.add(parsed.formatted());
        }

        return results;
    }

    public static synchronized List<LogEntry> getRecentEntries(int limit, String actorFilter) {
        if (config == null) {
            return List.of();
        }

        List<String> logs = config.getStringList(LOG_NODE);
        List<LogEntry> results = new ArrayList<>();

        for (int i = logs.size() - 1; i >= 0 && results.size() < limit; i--) {
            String entry = logs.get(i);
            ParsedLog parsed = parse(entry);
            if (parsed == null) {
                continue;
            }

            if (actorFilter != null && !parsed.actor.equalsIgnoreCase(actorFilter)) {
                continue;
            }

            results.add(new LogEntry(parsed.timestamp, parsed.status.equalsIgnoreCase("ALLOWED"), parsed.actor,
                    parsed.command));
        }

        return results;
    }

    public static synchronized List<String> getKnownActors() {
        if (config == null) {
            return List.of();
        }

        List<String> logs = config.getStringList(LOG_NODE);
        Set<String> actors = new LinkedHashSet<>();

        for (int i = logs.size() - 1; i >= 0; i--) {
            ParsedLog parsed = parse(logs.get(i));
            if (parsed != null) {
                actors.add(parsed.actor);
            }
        }

        return new ArrayList<>(actors);
    }

    private static ParsedLog parse(String raw) {
        try {
            int firstSpace = raw.indexOf(' ');
            int statusStart = raw.indexOf('[');
            int statusEnd = raw.indexOf(']');
            int actorStart = statusEnd + 2;
            int colonIndex = raw.indexOf(':', actorStart);

            if (firstSpace <= 0 || statusStart <= 0 || statusEnd <= statusStart || colonIndex <= actorStart) {
                return null;
            }

            LocalDateTime timestamp = LocalDateTime.parse(raw.substring(0, firstSpace), STORAGE_FORMAT);
            String status = raw.substring(statusStart + 1, statusEnd).trim();
            String actor = raw.substring(actorStart, colonIndex).trim();
            String command = raw.substring(colonIndex + 1).trim();

            return new ParsedLog(timestamp, status, actor, command);
        } catch (Exception e) {
            return null;
        }
    }

    public record LogEntry(LocalDateTime timestamp, boolean allowed, String actor, String command) {
        public String readableTime() {
            return timestamp.format(READABLE_FORMAT);
        }
    }

    private record ParsedLog(LocalDateTime timestamp, String status, String actor, String command) {
        String formatted() {
            String readableTime = timestamp.format(READABLE_FORMAT);
            String statusColor = status.equalsIgnoreCase("ALLOWED") ? "&a" : "&c";
            return "&7" + readableTime + " &8[ " + statusColor + status + "&8 ] &e" + actor + "&7: &f"
                    + command;
        }
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
