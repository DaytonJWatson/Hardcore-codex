package com.daytonjwatson.hardcore.config;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.daytonjwatson.hardcore.HardcorePlugin;

public final class ConfigValues {

    private static FileConfiguration config;

    private ConfigValues() {}

    public static void load(FileConfiguration configuration) {
        config = configuration;
    }

    public static boolean chatEnabled() {
        return config.getBoolean("chat.enabled", true);
    }

    public static String chatBanditPrefix() {
        return config.getString("chat.prefixes.bandit", "&4&l[B] &r");
    }

    public static String chatHeroPrefix() {
        return config.getString("chat.prefixes.hero", "&6&l[H] &r");
    }

    public static String chatNameColor() {
        return config.getString("chat.name-color", "&7");
    }

    public static String chatFormat() {
        return config.getString("chat.format", "%prefix%&7 <%name_color%%player%&7> %message%");
    }

    public static boolean tablistEnabled() {
        return config.getBoolean("tablist.enabled", true);
    }

    public static ConfigurationSection tabHeader() {
        return config.getConfigurationSection("tablist.header");
    }

    public static ConfigurationSection tabFooter() {
        return config.getConfigurationSection("tablist.footer");
    }

    public static boolean joinPanelEnabled() {
        return config.getBoolean("server.join-panel.enabled", true);
    }

    public static String joinPanelTitle() {
        return config.getString("server.join-panel.title", "Hardcore Mode");
    }

    public static List<String> joinPanelLines() {
        return config.getStringList("server.join-panel.lines");
    }

    public static String joinMessage(boolean firstJoin) {
        String path = firstJoin ? "server.first-join.join-message" : "server.returning.join-message";
        return config.getString(path, firstJoin ? "&7%player% has &ajoined &7for the first time!" : "&7%player% has &ajoined");
    }

    public static boolean giveGuideBookOnFirstJoin() {
        return config.getBoolean("server.first-join.guide-book.give-on-first-join", true);
    }

    public static boolean randomSpawnEnabled() {
        return config.getBoolean("server.first-join.random-spawn.enabled", true);
    }

    public static int randomSpawnRadius() {
        return config.getInt("server.first-join.random-spawn.radius", 5000);
    }

    public static int randomSpawnAttempts() {
        return config.getInt("server.first-join.random-spawn.max-attempts", 40);
    }

    public static Sound deathSound() {
        String soundName = config.getString("server.sounds.death", "ENTITY_WITHER_DEATH");
        Sound resolved = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
        if (resolved != null) {
            return resolved;
        }

        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException ex) {
            return Sound.ENTITY_WITHER_DEATH;
        }
    }

    public static int banditKillThreshold() {
        return config.getInt("bandits.thresholds.bandit-kills-to-flag", 3);
    }

    public static int redemptionKills() {
        return config.getInt("bandits.thresholds.redemption-kills", 3);
    }

    public static int heroKills() {
        return config.getInt("bandits.thresholds.hero-kills", 3);
    }

    public static int minKillerGearPower() {
        return config.getInt("bandits.unfair-kill-requirements.min-killer-gear-power", 6);
    }

    public static int minBaseGearGap() {
        return config.getInt("bandits.unfair-kill-requirements.min-base-gear-gap", 4);
    }

    public static double minEffectiveTotalGap() {
        return config.getDouble("bandits.unfair-kill-requirements.min-effective-total-gap", 3.0);
    }

    public static List<String> deathMessages(String key, List<String> defaults) {
        List<String> configured = config.getStringList("deaths.messages." + key);
        return configured.isEmpty() ? defaults : configured;
    }

    public static String statsFileName() {
        return config.getString("stats.file", "stats.yml");
    }

    public static long statsAutosaveDelayTicks() {
        return config.getLong("stats.autosave-delay-ticks", 100L);
    }

    public static boolean trackerEnabled() {
        return config.getBoolean("bandit-tracker.enabled", true);
    }

    public static String trackerDisplayName() {
        return config.getString("bandit-tracker.display-name", "&4&lBandit Tracker");
    }

    public static long trackerCooldownMillis() {
        return config.getLong("bandit-tracker.cooldown-seconds", 30L) * 1000L;
    }

    public static double trackerDriftScale() {
        return config.getDouble("bandit-tracker.drift.scale", 0.025);
    }

    public static double trackerMinDrift() {
        return config.getDouble("bandit-tracker.drift.min-blocks", 4.0);
    }

    public static List<String> trackerLoreBase() {
        return config.getStringList("bandit-tracker.lore.base");
    }

    public static String trackerLoreTrackingPrefix() {
        return config.getString("bandit-tracker.lore.tracking-prefix", "&8• &4Tracking: &cUnidentified bandit");
    }

    public static String trackerLoreDistance() {
        return config.getString("bandit-tracker.lore.distance", "&8• &7Distance: &f%meters%m");
    }

    public static String trackerLoreSignal() {
        return config.getString("bandit-tracker.lore.signal", "&8• &7Signal: %quality%");
    }

    public static String trackerLoreWorld() {
        return config.getString("bandit-tracker.lore.world", "&8• &7World: &f%world%");
    }

    public static String trackerLoreNoTarget() {
        return config.getString("bandit-tracker.lore.no-target", "&8• &cNo bandits online.");
    }

    public static String trackerSignalTight() {
        return config.getString("bandit-tracker.signals.tight", "&aTight&7 (low drift)");
    }

    public static String trackerSignalModerate() {
        return config.getString("bandit-tracker.signals.moderate", "&eModerate&7 (watch for drift)");
    }

    public static String trackerSignalLoose() {
        return config.getString("bandit-tracker.signals.loose", "&cLoose&7 (hunt carefully)");
    }

    public static ConfigurationSection trackerRecipe() {
        return config.getConfigurationSection("bandit-tracker.recipe");
    }

    public static String trackerRecipeTitle() {
        return config.getString("bandit-tracker.recipe.title", "&4&lTracker Recipe");
    }

    public static boolean trackerRecipeEnabled() {
        return config.getBoolean("bandit-tracker.recipe.enabled", true);
    }

    public static boolean trackerPreviewSlotHighlights() {
        return config.getBoolean("bandit-tracker.recipe.preview.slot-highlights", true);
    }

    public static List<String> trackerPreviewLore() {
        return config.getStringList("bandit-tracker.recipe.preview.tracker-lore");
    }

    public static String translateColor(String value) {
        return HardcorePlugin.getInstance() == null ? value : ChatColor.translateAlternateColorCodes('&', value);
    }

    public static Material materialOrNull(String name) {
        try {
            return Material.valueOf(name);
        } catch (Exception ex) {
            return null;
        }
    }
}
