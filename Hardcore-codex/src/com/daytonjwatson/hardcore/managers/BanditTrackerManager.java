package com.daytonjwatson.hardcore.managers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;

/**
 * Handles the bandit tracking compass lifecycle and targeting logic.
 */
public final class BanditTrackerManager {

    private static final String TRACKER_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Bandit Tracker";
    private static final NamespacedKey TRACKER_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bandit-tracker");
    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("0.0");

    private BanditTrackerManager() {}

    public static ItemStack createTrackerItem() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta baseMeta = compass.getItemMeta();

        if (baseMeta == null) {
            return compass;
        }

        PersistentDataContainer pdc = baseMeta.getPersistentDataContainer();
        pdc.set(TRACKER_KEY, PersistentDataType.INTEGER, 1);

        baseMeta.setDisplayName(TRACKER_TITLE);
        baseMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        baseMeta.setLore(buildBaseLore());

        compass.setItemMeta(baseMeta);
        return compass;
    }

    public static ItemStack findTracker(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isTracker(stack)) {
                return stack;
            }
        }
        return null;
    }

    public static boolean isTracker(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(TRACKER_KEY, PersistentDataType.INTEGER);
    }

    /**
     * Updates the given tracker to point at the closest bandit to the seeker.
     *
     * @return true if a bandit target was found and applied
     */
    public static boolean updateTracker(Player seeker, ItemStack tracker) {
        Player target = findNearestBandit(seeker);

        if (target == null) {
            writeNoTargetLore(tracker);
            return false;
        }

        Location targetLoc = target.getLocation();
        applyTargetToCompass(tracker, seeker, target, targetLoc);
        seeker.setCompassTarget(targetLoc);
        return true;
    }

    public static Player findNearestBandit(Player seeker) {
        StatsManager stats = StatsManager.get();
        if (stats == null) {
            return null;
        }

        Player nearestSameWorld = null;
        double nearestDistSq = Double.MAX_VALUE;
        Player nearestAnyWorld = null;

        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getUniqueId().equals(seeker.getUniqueId())) {
                continue;
            }

            UUID uuid = candidate.getUniqueId();
            if (!stats.isBandit(uuid)) {
                continue;
            }

            if (candidate.getWorld().equals(seeker.getWorld())) {
                double distSq = seeker.getLocation().distanceSquared(candidate.getLocation());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestSameWorld = candidate;
                }
            } else if (nearestAnyWorld == null) {
                nearestAnyWorld = candidate;
            }
        }

        return nearestSameWorld != null ? nearestSameWorld : nearestAnyWorld;
    }

    private static void applyTargetToCompass(ItemStack tracker, Player seeker, Player target, Location targetLoc) {
        ItemMeta meta = tracker.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> lore = buildBaseLore();

        lore.add(MessageStyler.bulletText(ChatColor.DARK_RED + "Tracking: " + ChatColor.RED + target.getName()));

        if (seeker.getWorld().equals(targetLoc.getWorld())) {
            double distance = seeker.getLocation().distance(targetLoc);
            lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Distance: " + ChatColor.WHITE +
                    DISTANCE_FORMAT.format(distance) + ChatColor.GRAY + "m"));
        } else {
            lore.add(MessageStyler.bulletText(ChatColor.GRAY + "World: " + ChatColor.WHITE + targetLoc.getWorld().getName()));
        }

        meta.setLore(lore);

        if (meta instanceof CompassMeta) {
            CompassMeta compassMeta = (CompassMeta) meta;
            compassMeta.setLodestone(targetLoc);
            compassMeta.setLodestoneTracked(false);
            tracker.setItemMeta(compassMeta);
        } else {
            tracker.setItemMeta(meta);
        }
    }

    private static void writeNoTargetLore(ItemStack tracker) {
        ItemMeta meta = tracker.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> lore = buildBaseLore();
        lore.add(MessageStyler.bulletText(ChatColor.RED + "No bandits online."));
        meta.setLore(lore);
        tracker.setItemMeta(meta);
    }

    private static List<String> buildBaseLore() {
        List<String> lore = new ArrayList<>();
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Right-click to lock onto"));
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "the nearest " + ChatColor.DARK_RED + "Bandit" + ChatColor.GRAY + "."));
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Compass auto-updates on use."));
        return lore;
    }
}
