package com.daytonjwatson.hardcore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {

    private static StatsManager instance;

    private final JavaPlugin plugin;
    private File statsFile;
    private FileConfiguration statsConfig;

    // Global stats
    private final Set<UUID> uniquePlayers = new HashSet<>();
    private int totalDeaths = 0;

    // Per-player stats
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Long> firstJoin = new HashMap<>();
    private final Map<UUID, Long> lastDeath = new HashMap<>();

    // Bandit tracking
    private final Set<UUID> bandits = new HashSet<>();
    private final Map<UUID, Integer> banditKills = new HashMap<>();

    // Redemption: bandits killing bandits
    private final Map<UUID, Integer> banditHunterKills = new HashMap<>();

    // Hero tracking: non-bandits killing bandits
    private final Set<UUID> heroes = new HashSet<>();
    private final Map<UUID, Integer> heroBanditKills = new HashMap<>();

    // Players who have ever been bandits (cannot become heroes)
    private final Set<UUID> everBandits = new HashSet<>();

    private StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createFile();
        loadData();
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new StatsManager(plugin);
        }
    }

    public static StatsManager get() {
        return instance;
    }

    private void createFile() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");

        if (!statsFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    private void loadData() {
        List<String> uuidStrings = statsConfig.getStringList("unique-players");
        for (String s : uuidStrings) {
            try {
                uniquePlayers.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {}
        }

        totalDeaths = statsConfig.getInt("total-deaths", 0);

        if (statsConfig.isConfigurationSection("players")) {
            for (String uuidStr : statsConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String base = "players." + uuidStr + ".";

                    int d  = statsConfig.getInt(base + "deaths", 0);
                    int k  = statsConfig.getInt(base + "kills", 0);
                    long fj = statsConfig.getLong(base + "first-join", 0L);
                    long ld = statsConfig.getLong(base + "last-death", 0L);

                    int bk  = statsConfig.getInt(base + "bandit-kills", 0);
                    boolean isBandit = statsConfig.getBoolean(base + "bandit", false);

                    int bhk = statsConfig.getInt(base + "bandit-hunter-kills", 0);

                    int hbKills = statsConfig.getInt(base + "hero-bandit-kills", 0);
                    boolean isHero = statsConfig.getBoolean(base + "hero", false);

                    boolean wasEverBandit = statsConfig.getBoolean(base + "ever-bandit", false);

                    if (d  > 0) deaths.put(uuid, d);
                    if (k  > 0) kills.put(uuid, k);
                    if (fj > 0) firstJoin.put(uuid, fj);
                    if (ld > 0) lastDeath.put(uuid, ld);

                    if (bk > 0) banditKills.put(uuid, bk);
                    if (isBandit) bandits.add(uuid);

                    if (bhk > 0) banditHunterKills.put(uuid, bhk);

                    if (hbKills > 0) heroBanditKills.put(uuid, hbKills);
                    if (isHero) heroes.add(uuid);

                    if (wasEverBandit) everBandits.add(uuid);
                    if (isBandit) everBandits.add(uuid); // ensure current bandits are marked as ever-bandits

                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        List<String> uuidStrings = uniquePlayers.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());

        statsConfig.set("unique-players", uuidStrings);
        statsConfig.set("total-deaths", totalDeaths);

        for (UUID uuid : uniquePlayers) {
            String base = "players." + uuid.toString() + ".";

            statsConfig.set(base + "deaths", deaths.getOrDefault(uuid, 0));
            statsConfig.set(base + "kills", kills.getOrDefault(uuid, 0));
            statsConfig.set(base + "first-join", firstJoin.getOrDefault(uuid, 0L));
            statsConfig.set(base + "last-death", lastDeath.getOrDefault(uuid, 0L));

            statsConfig.set(base + "bandit-kills", banditKills.getOrDefault(uuid, 0));
            statsConfig.set(base + "bandit", bandits.contains(uuid));

            statsConfig.set(base + "bandit-hunter-kills", banditHunterKills.getOrDefault(uuid, 0));

            statsConfig.set(base + "hero-bandit-kills", heroBanditKills.getOrDefault(uuid, 0));
            statsConfig.set(base + "hero", heroes.contains(uuid));

            boolean everB = everBandits.contains(uuid) || bandits.contains(uuid);
            statsConfig.set(base + "ever-bandit", everB);
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hooks

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (uniquePlayers.add(uuid)) {
            firstJoin.put(uuid, now);
        } else {
            firstJoin.putIfAbsent(uuid, now);
        }

        saveData();
    }

    public void handleDeath(Player victim, Player killer) {
        UUID vUuid = victim.getUniqueId();
        long now = System.currentTimeMillis();

        totalDeaths++;

        deaths.put(vUuid, deaths.getOrDefault(vUuid, 0) + 1);
        lastDeath.put(vUuid, now);

        if (killer != null) {
            UUID kUuid = killer.getUniqueId();
            kills.put(kUuid, kills.getOrDefault(kUuid, 0) + 1);
            uniquePlayers.add(kUuid);
            firstJoin.putIfAbsent(kUuid, now);
        }

        saveData();
    }

    /**
     * Called when an unfair kill is detected.
     * Requires 3+ unfair kills to become a bandit.
     */
    public void handleUnfairKill(UUID killerUuid) {
        int current = banditKills.getOrDefault(killerUuid, 0) + 1;
        banditKills.put(killerUuid, current);

        if (current >= 3) {
            // Mark as bandit
            bandits.add(killerUuid);
            everBandits.add(killerUuid); // mark as ever-bandit

            // If they were a Hero, strip Hero status on becoming a Bandit
            if (heroes.remove(killerUuid)) {
                heroBanditKills.put(killerUuid, 0); // reset hero progress if you want
            }
        }

        saveData();
    }

    /**
     * Called when a BANDIT kills a BANDIT (for redemption).
     * If the killer is a bandit and reaches 3 bandit kills,
     * they lose their bandit status.
     *
     * @return true if this call caused the killer to lose bandit status
     */
    public boolean handleBanditKill(UUID killerUuid) {
        // Only track redemption for players who are currently bandits
        if (!isBandit(killerUuid)) {
            return false;
        }

        int current = banditHunterKills.getOrDefault(killerUuid, 0) + 1;
        banditHunterKills.put(killerUuid, current);

        boolean lostBandit = false;

        if (current >= 3) {
            bandits.remove(killerUuid);
            banditKills.put(killerUuid, 0);
            banditHunterKills.put(killerUuid, 0);
            // NOTE: everBandits is NOT cleared – they can never be a Hero
            lostBandit = true;
        }

        saveData();
        return lostBandit;
    }

    /**
     * Called when a NON-BANDIT kills a BANDIT.
     * If the killer is NOT currently a bandit and reaches 3 bandit kills,
     * they become a Hero.
     *
     * Players who have been bandits in the past can still become Heroes,
     * but only after they are no longer bandits (Bandit -> Regular -> Hero).
     *
     * @return true if this call caused the killer to become a Hero
     */
    public boolean handleHeroBanditKill(UUID killerUuid) {
        // Cannot progress toward Hero while currently a bandit
        if (isBandit(killerUuid)) {
            return false;
        }

        int current = heroBanditKills.getOrDefault(killerUuid, 0) + 1;
        heroBanditKills.put(killerUuid, current);

        boolean becameHero = false;

        if (current >= 3 && !isHero(killerUuid)) {
            heroes.add(killerUuid);
            becameHero = true;
        }

        saveData();
        return becameHero;
    }


    // Getters

    public int getUniquePlayerCount() {
        return uniquePlayers.size();
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getDeaths(UUID uuid) {
        return deaths.getOrDefault(uuid, 0);
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public long getFirstJoin(UUID uuid) {
        return firstJoin.getOrDefault(uuid, 0L);
    }

    public long getLastDeath(UUID uuid) {
        return lastDeath.getOrDefault(uuid, 0L);
    }

    /**
     * Life length:
     *  - if dead: lastDeath - firstJoin
     *  - if alive: now - firstJoin
     */
    public long getLifeLengthMillis(UUID uuid, boolean isAlive) {
        long fj = getFirstJoin(uuid);
        if (fj == 0L) return 0L;

        if (!isAlive) {
            long ld = getLastDeath(uuid);
            if (ld == 0L) return 0L;
            return Math.max(0L, ld - fj);
        } else {
            return Math.max(0L, System.currentTimeMillis() - fj);
        }
    }

    public boolean isBandit(UUID uuid) {
        return bandits.contains(uuid);
    }

    public int getBanditKills(UUID uuid) {
        return banditKills.getOrDefault(uuid, 0);
    }

    public int getBanditHunterKills(UUID uuid) {
        return banditHunterKills.getOrDefault(uuid, 0);
    }

    public boolean isHero(UUID uuid) {
        return heroes.contains(uuid);
    }

    public int getHeroBanditKills(UUID uuid) {
        return heroBanditKills.getOrDefault(uuid, 0);
    }

    public boolean hasEverBeenBandit(UUID uuid) {
        return everBandits.contains(uuid) || bandits.contains(uuid);
    }
}
