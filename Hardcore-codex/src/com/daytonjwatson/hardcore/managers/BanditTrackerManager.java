package com.daytonjwatson.hardcore.managers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;

/**
 * Handles the bandit tracking compass lifecycle and targeting logic.
 */
public final class BanditTrackerManager {

    private static final String TRACKER_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Bandit Tracker";
    private static final NamespacedKey TRACKER_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "bandit-tracker");
    private static final NamespacedKey TRACKER_RECIPE_KEY = new NamespacedKey(HardcorePlugin.getInstance(),
            "bandit-tracker-recipe");
    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("0.0");
    private static final long TRACKER_COOLDOWN_MILLIS = 30_000L;
    private static final double DRIFT_SCALE = 0.025; // 2.5% of distance with a minimum floor
    private static final double MIN_DRIFT = 4.0; // blocks
    private static final Map<UUID, Long> TRACKER_COOLDOWNS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final String RECIPE_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tracker Recipe";
    private static Inventory recipePreview;

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

    public static void openRecipePreview(Player player) {
        if (recipePreview == null) {
            recipePreview = buildRecipePreview();
        }

        player.openInventory(recipePreview);
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

    public static void registerRecipe(JavaPlugin plugin) {
        ShapedRecipe recipe = new ShapedRecipe(TRACKER_RECIPE_KEY, createTrackerItem());
        recipe.shape("RGR", "ECE", "RIR");
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('C', Material.COMPASS);
        recipe.setIngredient('I', Material.IRON_INGOT);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("Bandit tracker recipe registered.");
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
        Location driftedTarget = applyTargetToCompass(tracker, seeker, target, targetLoc);
        seeker.setCompassTarget(driftedTarget);
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

    public static boolean isOnCooldown(Player player) {
        Long lastUse = TRACKER_COOLDOWNS.get(player.getUniqueId());
        if (lastUse == null) {
            return false;
        }

        return System.currentTimeMillis() - lastUse < TRACKER_COOLDOWN_MILLIS;
    }

    public static long getRemainingCooldownSeconds(Player player) {
        Long lastUse = TRACKER_COOLDOWNS.get(player.getUniqueId());
        if (lastUse == null) {
            return 0L;
        }

        long elapsed = System.currentTimeMillis() - lastUse;
        long remainingMillis = TRACKER_COOLDOWN_MILLIS - elapsed;
        return Math.max(0L, Math.round(Math.ceil(remainingMillis / 1000.0)));
    }

    public static void markTrackerUsed(Player player) {
        TRACKER_COOLDOWNS.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private static Location applyTargetToCompass(ItemStack tracker, Player seeker, Player target, Location targetLoc) {
        ItemMeta meta = tracker.getItemMeta();
        if (meta == null) {
            return targetLoc;
        }

        List<String> lore = buildBaseLore();

        lore.add(MessageStyler.bulletText(ChatColor.DARK_RED + "Tracking: " + ChatColor.RED + "Unidentified bandit"));

        if (seeker.getWorld().equals(targetLoc.getWorld())) {
            double distance = seeker.getLocation().distance(targetLoc);
            targetLoc = applySignalDrift(targetLoc, distance);
            lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Distance: " + ChatColor.WHITE
                    + DISTANCE_FORMAT.format(distance) + ChatColor.GRAY + "m"));
            lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Signal: " + describeSignal(distance)));
        } else {
            lore.add(MessageStyler.bulletText(ChatColor.GRAY + "World: " + ChatColor.WHITE + targetLoc.getWorld().getName()));
            lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Signal: " + ChatColor.RED + "Unstable"));
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

        return targetLoc;
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

    private static Location applySignalDrift(Location target, double distance) {
        double drift = Math.max(MIN_DRIFT, distance * DRIFT_SCALE);
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double offset = drift * RANDOM.nextDouble();

        double xOffset = Math.cos(angle) * offset;
        double zOffset = Math.sin(angle) * offset;

        return target.clone().add(xOffset, 0, zOffset);
    }

    private static String describeSignal(double distance) {
        if (distance < 75) {
            return ChatColor.GREEN + "Tight" + ChatColor.GRAY + " (low drift)";
        }

        if (distance < 200) {
            return ChatColor.YELLOW + "Moderate" + ChatColor.GRAY + " (watch for drift)";
        }

        return ChatColor.RED + "Loose" + ChatColor.GRAY + " (hunt carefully)";
    }

    private static List<String> buildBaseLore() {
        List<String> lore = new ArrayList<>();
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Sneak-right-click to lock onto"));
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "the nearest " + ChatColor.DARK_RED + "Bandit" + ChatColor.GRAY + "."));
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Signal drifts with range; recalibrate often."));
        lore.add(MessageStyler.bulletText(ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + "30s" + ChatColor.GRAY + "."));
        return lore;
    }

    private static Inventory buildRecipePreview() {
        InventoryHolder holder = new RecipeInventoryHolder();
        Inventory inv = Bukkit.createInventory(holder, InventoryType.CHEST, RECIPE_TITLE);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + "");
            fillerMeta.addItemFlags(ItemFlag.values());
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Lay out the shaped recipe in a centered 3x3 grid
        inv.setItem(2, new ItemStack(Material.REDSTONE));
        inv.setItem(3, new ItemStack(Material.GOLD_INGOT));
        inv.setItem(4, new ItemStack(Material.REDSTONE));

        inv.setItem(11, new ItemStack(Material.ENDER_PEARL));
        inv.setItem(12, new ItemStack(Material.COMPASS));
        inv.setItem(13, new ItemStack(Material.ENDER_PEARL));

        inv.setItem(20, new ItemStack(Material.REDSTONE));
        inv.setItem(21, new ItemStack(Material.IRON_INGOT));
        inv.setItem(22, new ItemStack(Material.REDSTONE));

        ItemStack tracker = createTrackerItem();
        ItemMeta trackerMeta = tracker.getItemMeta();
        if (trackerMeta != null) {
            trackerMeta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            trackerMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            trackerMeta.setLore(List.of(
                    ChatColor.GRAY + "Craft your own tracker.",
                    ChatColor.DARK_GRAY + "Sneak-right-click to calibrate."));
            tracker.setItemMeta(trackerMeta);
        }

        inv.setItem(24, tracker);
        return inv;
    }

    public static boolean isRecipeInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof RecipeInventoryHolder;
    }

    private static final class RecipeInventoryHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return recipePreview;
        }
    }
}
