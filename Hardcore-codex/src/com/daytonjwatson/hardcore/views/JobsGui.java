package com.daytonjwatson.hardcore.views;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.jobs.Occupation;
import com.daytonjwatson.hardcore.jobs.JobsManager.OccupationProfile;
import com.daytonjwatson.hardcore.jobs.JobsManager.OccupationSettings;
import com.daytonjwatson.hardcore.jobs.JobsManager.PayoutRecord;
import com.daytonjwatson.hardcore.utils.Util;

public final class JobsGui {

    public static final String TITLE = Util.color("&6&lOccupations &8| &7Choose your path");
    public static final String TITLE_SUMMARY = Util.color("&6&lOccupation &8| &7Your stats");
    public static final String TITLE_CONFIRM = Util.color("&6&lOccupation &8| &cConfirm choice");
    public static final int CONFIRM_SLOT = 11;
    public static final int CANCEL_SLOT = 15;

    private static final int[] OCCUPATION_SLOTS = {10, 12, 14, 16, 28, 30, 32};
    private static final DecimalFormat CURRENCY = new DecimalFormat("###,###.##");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    private JobsGui() {
    }

    public static boolean isJobsMenu(String title) {
        return TITLE.equals(title) || TITLE_SUMMARY.equals(title) || TITLE_CONFIRM.equals(title);
    }

    public static boolean isConfirm(String title) {
        return TITLE_CONFIRM.equals(title);
    }

    public static int[] getOccupationSlots() {
        return OCCUPATION_SLOTS;
    }

    public static void open(Player player) {
        JobsManager jobs = JobsManager.get();
        Occupation current = jobs.getOccupation(player.getUniqueId());
        if (current == null) {
            openChooser(player);
        } else {
            openSummary(player, current, jobs.getProfile(player.getUniqueId()));
        }
    }

    public static void openConfirm(Player player, Occupation occupation) {
        JobsManager jobs = JobsManager.get();
        OccupationSettings settings = jobs.getOccupationSettings().get(occupation);
        Inventory menu = Bukkit.createInventory(null, 27, TITLE_CONFIRM);
        fill(menu, item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        List<String> lore = new ArrayList<>();
        lore.add("&7This choice is &cPERMANENT&7.");
        lore.add("&7You cannot switch jobs later.");
        lore.add(" ");
        for (String line : settings.description()) {
            lore.add("&7" + line);
        }
        menu.setItem(13, item(settings.icon(), "&6Confirm " + settings.displayName(), lore));
        menu.setItem(CONFIRM_SLOT, item(Material.LIME_WOOL, "&aConfirm", List.of("&7Select this occupation.")));
        menu.setItem(CANCEL_SLOT, item(Material.RED_WOOL, "&cGo Back", List.of("&7Return to the list.")));
        player.openInventory(menu);
    }

    private static void openChooser(Player player) {
        JobsManager jobs = JobsManager.get();
        Inventory menu = Bukkit.createInventory(null, 54, TITLE);
        fill(menu, item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));

        int index = 0;
        for (Occupation occupation : Occupation.values()) {
            if (index >= OCCUPATION_SLOTS.length) {
                break;
            }
            OccupationSettings settings = jobs.getOccupationSettings().get(occupation);
            menu.setItem(OCCUPATION_SLOTS[index], summarizeOccupation(occupation, settings));
            index++;
        }

        menu.setItem(49, item(Material.BARRIER, "&cPermanent Choice", List.of(
                "&7You only get one occupation.",
                "&7Choose carefully before confirming.")));
        player.openInventory(menu);
    }

    private static void openSummary(Player player, Occupation occupation, OccupationProfile profile) {
        JobsManager jobs = JobsManager.get();
        OccupationSettings settings = jobs.getOccupationSettings().get(occupation);
        Inventory menu = Bukkit.createInventory(null, 54, TITLE_SUMMARY);
        fill(menu, item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));

        menu.setItem(22, summarizeOccupation(occupation, settings));
        List<String> statsLore = new ArrayList<>();
        statsLore.add("&fChosen: &7" + DATE_FORMAT.format(new Date(profile.getChosenAt())));
        statsLore.add("&fLifetime: &a$" + CURRENCY.format(profile.getLifetimeEarnings()));
        statsLore.add("&fSession: &a$" + CURRENCY.format(profile.getSessionEarnings()));
        statsLore.add(" ");
        statsLore.add("&fDaily cap remaining: &a$" + CURRENCY
                .format(Math.max(0, settings.getDailyCap() - profile.getDailyEarnings())));
        statsLore.add(" ");
        statsLore.add("&6Recent payouts:");
        int shown = 0;
        for (PayoutRecord record : profile.getRecentPayouts()) {
            statsLore.add("&7- &a$" + CURRENCY.format(record.amount()) + " &f" + record.reason());
            if (++shown >= 5) {
                break;
            }
        }
        menu.setItem(31, item(Material.BOOK, "&eYour Stats", statsLore));
        player.openInventory(menu);
    }

    private static void fill(Inventory inventory, ItemStack filler) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private static ItemStack summarizeOccupation(Occupation occupation, OccupationSettings settings) {
        List<String> lore = new ArrayList<>();
        lore.add("&7This choice is permanent.");
        lore.add(" ");
        for (String line : settings.description()) {
            lore.add("&7" + line);
        }
        lore.add(" ");
        switch (occupation) {
            case WARRIOR -> {
                lore.add("&fPvP kill: &a+" + settings.getReward("pvp-kill"));
                lore.add("&fHostile kill: &a+" + settings.getReward("hostile-kill"));
            }
            case FARMER -> lore.add("&fHarvest: &a+" + settings.getReward("harvest"));
            case FISHERMAN -> lore.add("&fCatch: &a+" + settings.getReward("catch"));
            case LUMBERJACK -> lore.add("&fTree: &a+" + settings.getReward("tree"));
            case MINER -> {
                lore.add("&fOre: &a+" + settings.getReward("ore"));
                lore.add("&fDepth bonus: &a+" + settings.getReward("depth-bonus"));
            }
            case EXPLORER -> lore.add("&fNew chunk: &a+" + settings.getReward("chunk"));
            case BUILDER -> lore.add("&fBuild session: &a+" + settings.getReward("build-session"));
        }
        lore.add("&fDaily cap: &a$" + settings.getDailyCap());
        lore.add(" ");
        lore.add("&eClick to select.");

        return item(settings.icon(), "&6" + settings.displayName(), lore);
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
}
