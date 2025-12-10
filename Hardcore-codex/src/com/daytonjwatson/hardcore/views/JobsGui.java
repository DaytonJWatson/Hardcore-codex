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
import com.daytonjwatson.hardcore.jobs.ActiveObjective;
import com.daytonjwatson.hardcore.jobs.JobDefinition;
import com.daytonjwatson.hardcore.jobs.JobObjective;
import com.daytonjwatson.hardcore.jobs.JobOffer;
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

        List<JobOffer> offers = jobs.getOfferedJobs(player.getUniqueId());
        int[] slots = {11, 13, 15};
        for (int i = 0; i < offers.size() && i < slots.length; i++) {
            boolean cooling = jobs.isSlotCoolingDown(player.getUniqueId(), i);
            JobOffer offer = offers.get(i);
            if (cooling) {
                long remaining = jobs.getCooldownRemainingMillis(player.getUniqueId(), i);
                menu.setItem(slots[i], cooldownItem(i + 1, remaining));
            } else if (offer != null) {
                menu.setItem(slots[i], summarizeJob(offer, i + 1));
            }
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

    private static ItemStack summarizeJob(JobOffer offer, int optionNumber) {
        JobDefinition definition = offer.getDefinition();
        List<String> lore = new ArrayList<>();
        for (String line : definition.getDescription()) {
            lore.add("&7" + line);
        }
        lore.add(" ");
        for (int i = 0; i < definition.getObjectives().size(); i++) {
            JobObjective objective = definition.getObjectives().get(i);
            lore.add("&fObjective " + (i + 1) + ": &e" + formatNumber(offer.getAmount(i)) + "x &f"
                    + objective.getTarget());
            lore.add("&7 - Type: &6" + objective.getType().name().replace('_', ' '));
            if (objective.shouldConsumeItems()) {
                lore.add("&7 - Consumes items on progress.");
            }
        }
        lore.add("&fOrder: &6" + (definition.isOrdered() ? "Sequential" : "Concurrent"));
        lore.add("&fDifficulty: &c" + definition.getDifficulty());
        lore.add("&fReward: &a" + definition.getReward());
        lore.add(" ");
        lore.add("&eClick to accept option " + optionNumber + "!");

        Material icon = Material.WRITABLE_BOOK;
        JobObjective primary = definition.getPrimaryObjective();
        if (primary != null) {
            icon = switch (primary.getType()) {
                case KILL_MOB -> Material.IRON_SWORD;
                case COLLECT_ITEM -> Material.CHEST;
                case MINE_BLOCK -> Material.DIAMOND_PICKAXE;
                case FISH_ITEM -> Material.FISHING_ROD;
                case CRAFT_ITEM -> Material.CRAFTING_TABLE;
                case PLACE_BLOCK -> Material.GRASS_BLOCK;
                case SMELT_ITEM -> Material.BLAST_FURNACE;
                case ENCHANT_ITEM -> Material.ENCHANTING_TABLE;
                case BREED_ANIMAL -> Material.WHEAT;
                case TAME_ENTITY -> Material.LEAD;
                case TRAVEL_BIOME -> Material.COMPASS;
                case TRAVEL_DISTANCE -> Material.ELYTRA;
            };
        }
        return item(icon, "&6" + definition.getDisplayName(), lore);
    }

    private static ItemStack cooldownItem(int optionNumber, long remainingMs) {
        List<String> lore = new ArrayList<>();
        lore.add("&7This option is cooling down.");
        lore.add("&7Available in: &c" + formatDuration(remainingMs));
        return item(Material.BARRIER, "&cOption " + optionNumber + " Locked", lore);
    }

    private static ItemStack activeJobItem(ActiveJob active) {
        JobDefinition job = active.getJob();
        List<String> lore = new ArrayList<>();
        int index = 1;
        for (ActiveObjective objective : active.getObjectives()) {
            lore.add("&7Obj " + index + ": &f" + formatNumber(objective.getProgress()) + "/"
                    + formatNumber(objective.getGoalAmount()) + " " + objective.getDefinition().getTarget());
            index++;
        }
        lore.add("&7Order: &f" + (job.isOrdered() ? "Sequential" : "Concurrent"));
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

    private static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        return remainingSeconds + "s";
    }
}
