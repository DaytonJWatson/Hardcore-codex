package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class PlayerIpManager {

    private static final String PLAYERS_NODE = "players";
    private static final String NAME_NODE = "name";
    private static final String LAST_IP_NODE = "last-ip";
    private static final String LAST_SEEN_NODE = "last-seen";
    private static final String IP_HISTORY_NODE = "ip-history";

    private static FileConfiguration config;
    private static File file;

    private PlayerIpManager() {}

    public static void init(HardcorePlugin plugin) {
        file = new File(plugin.getDataFolder(), "player_ips.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            config = new YamlConfiguration();
            config.createSection(PLAYERS_NODE);
            save();
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection(PLAYERS_NODE)) {
            config.createSection(PLAYERS_NODE);
            save();
        }
    }

    public static void recordLogin(Player player, SocketAddress address) {
        if (config == null || address == null) {
            return;
        }

        InetAddress inetAddress = null;
        if (address instanceof InetSocketAddress inetSocketAddress) {
            inetAddress = inetSocketAddress.getAddress();
        } else if (address instanceof InetAddress directInetAddress) {
            inetAddress = directInetAddress;
        }

        if (inetAddress == null) {
            return;
        }

        String ip = inetAddress.getHostAddress();
        UUID uuid = player.getUniqueId();
        String node = PLAYERS_NODE + "." + uuid;

        config.set(node + "." + NAME_NODE, player.getName());
        config.set(node + "." + LAST_IP_NODE, ip);
        config.set(node + "." + LAST_SEEN_NODE, System.currentTimeMillis());

        List<String> history = new ArrayList<>(config.getStringList(node + "." + IP_HISTORY_NODE));
        if (!history.contains(ip)) {
            history.add(ip);
        }
        config.set(node + "." + IP_HISTORY_NODE, history);

        save();
    }

    public static String getLastIp(UUID uuid) {
        return config == null ? null : config.getString(PLAYERS_NODE + "." + uuid + "." + LAST_IP_NODE);
    }

    public static long getLastSeen(UUID uuid) {
        return config == null ? 0L : config.getLong(PLAYERS_NODE + "." + uuid + "." + LAST_SEEN_NODE, 0L);
    }

    public static List<String> getIpHistory(UUID uuid) {
        if (config == null) {
            return List.of();
        }
        return new ArrayList<>(config.getStringList(PLAYERS_NODE + "." + uuid + "." + IP_HISTORY_NODE));
    }

    public static String getStoredName(UUID uuid) {
        if (config == null) {
            return null;
        }
        return config.getString(PLAYERS_NODE + "." + uuid + "." + NAME_NODE);
    }

    public static List<String> getAltsFor(UUID uuid) {
        if (config == null) {
            return List.of();
        }

        String ip = getLastIp(uuid);
        if (ip == null || ip.isEmpty()) {
            return List.of();
        }

        ConfigurationSection playersSection = config.getConfigurationSection(PLAYERS_NODE);
        if (playersSection == null) {
            return List.of();
        }

        Set<String> keys = playersSection.getKeys(false);
        List<String> alts = new ArrayList<>();

        for (String key : keys) {
            if (key.equals(uuid.toString())) {
                continue;
            }

            String otherIp = playersSection.getString(key + "." + LAST_IP_NODE);
            if (ip.equals(otherIp)) {
                String name = playersSection.getString(key + "." + NAME_NODE, "Unknown");
                alts.add(name + " (" + key + ")");
            }
        }

        return alts;
    }

    public static List<String> getStoredNames() {
        if (config == null) {
            return List.of();
        }

        ConfigurationSection playersSection = config.getConfigurationSection(PLAYERS_NODE);
        if (playersSection == null) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (String key : playersSection.getKeys(false)) {
            String name = playersSection.getString(key + "." + NAME_NODE);
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }
        }

        return names;
    }

    public static OfflinePlayer resolveByName(String name) {
        if (config == null) {
            return null;
        }

        ConfigurationSection playersSection = config.getConfigurationSection(PLAYERS_NODE);
        if (playersSection == null) {
            return null;
        }

        Set<String> keys = playersSection.getKeys(false);
        for (String key : keys) {
            if (name.equalsIgnoreCase(playersSection.getString(key + "." + NAME_NODE))) {
                return HardcorePlugin.getInstance().getServer().getOfflinePlayer(UUID.fromString(key));
            }
        }
        return null;
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
