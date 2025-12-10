package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.daytonjwatson.hardcore.jobs.ActiveJob;
import com.daytonjwatson.hardcore.jobs.JobDefinition;
import com.daytonjwatson.hardcore.jobs.JobType;
import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class JobsGui {

    public static final String TITLE = Util.color("&6&lJobs &8| &7Pick a contract");
    public static final String ACTIVE_TITLE = Util.color("&6&lJobs &8| &7Active job");

    private JobsGui() {
    }

    public static void open(Player player) {
        JobsManager jobs = JobsManager.get();
        ActiveJob active = jobs.getActiveJob(player.getUniqueId());

        Inventory menu = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler);
        }

        List<JobDefinition> offers = jobs.getOfferedJobs(player.getUniqueId());
        int[] slots = {11, 13, 15};
        for (int i = 0; i < offers.size() && i < slots.length; i++) {
            JobDefinition job = offers.get(i);
            menu.setItem(slots[i], summarizeJob(job, i + 1));
        }

        if (active != null) {
            menu.setItem(26, activeJobItem(active));
        } else {
            menu.setItem(26, item(Material.BARRIER, "&cNo Active Job", List.of("&7Accept a contract to begin.")));
        }

        player.openInventory(menu);
    }

    public static void openActive(Player player) {
        JobsManager jobs = JobsManager.get();
        ActiveJob active = jobs.getActiveJob(player.getUniqueId());
        Inventory menu = Bukkit.createInventory(null, 27, ACTIVE_TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler);
        }

        if (active != null) {
            menu.setItem(13, activeJobItem(active));
            menu.setItem(22, item(Material.BARRIER, "&cAbandon Job", List.of("&7Click to abandon your active job.")));
        } else {
            menu.setItem(13, item(Material.BOOK, "&eNo Active Job", List.of("&7Use /jobs to accept a contract.")));
        }

        player.openInventory(menu);
    }

    private static ItemStack summarizeJob(JobDefinition definition, int optionNumber) {
        List<String> lore = new ArrayList<>();
        for (String line : definition.getDescription()) {
            lore.add("&7" + line);
        }
        lore.add(" ");
        lore.add("&fGoal: &e" + formatNumber(definition.getAmount()) + "x &f" + definition.getTarget());
        lore.add("&fType: &6" + definition.getType().name().replace('_', ' '));
        lore.add("&fDifficulty: &c" + definition.getDifficulty());
        lore.add("&fReward: &a" + definition.getReward());
        lore.add(" ");
        lore.add("&eClick to accept option " + optionNumber + "!");

        Material icon = switch (definition.getType()) {
            case KILL_MOB -> Material.IRON_SWORD;
            case COLLECT_ITEM -> Material.CHEST;
            case MINE_BLOCK -> Material.DIAMOND_PICKAXE;
            case FISH_ITEM -> Material.FISHING_ROD;
            case CRAFT_ITEM -> Material.CRAFTING_TABLE;
            case TRAVEL_BIOME -> Material.COMPASS;
            case TRAVEL_DISTANCE -> Material.ELYTRA;
        };
        return item(icon, "&6" + definition.getDisplayName(), lore);
    }

    private static ItemStack activeJobItem(ActiveJob active) {
        JobDefinition job = active.getJob();
        List<String> lore = new ArrayList<>();
        lore.add("&7Progress: &f" + formatNumber(active.getProgress()) + "/" + formatNumber(job.getAmount()));
        lore.add("&7Target: &f" + job.getTarget());
        lore.add("&7Reward: &a" + job.getReward());
        return item(Material.NETHER_STAR, "&bActive: " + job.getDisplayName(), lore);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Util.color(line));
            }
            meta.setLore(coloredLore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatNumber(double value) {
        if (value % 1 == 0) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}
