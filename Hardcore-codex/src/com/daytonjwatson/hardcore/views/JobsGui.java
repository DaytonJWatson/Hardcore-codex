package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.jobs.Occupation;
import com.daytonjwatson.hardcore.jobs.JobsManager.OccupationSettings;
import com.daytonjwatson.hardcore.utils.Util;

public final class JobsGui {

    public static final String TITLE = Util.color("&6&lOccupations &8| &7Choose your path");

    private JobsGui() {
    }

    public static void open(Player player) {
        JobsManager jobs = JobsManager.get();
        Inventory menu = Bukkit.createInventory(null, 54, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler);
        }

        Occupation current = jobs.getOccupation(player.getUniqueId());
        int[] slots = {10, 12, 14, 16, 28, 30, 32};
        int index = 0;
        for (Occupation occupation : Occupation.values()) {
            if (index >= slots.length) {
                break;
            }
            OccupationSettings settings = jobs.getOccupationSettings().get(occupation);
            menu.setItem(slots[index], summarizeOccupation(occupation, settings, current == occupation));
            index++;
        }

        if (current != null) {
            menu.setItem(49, item(Material.BARRIER, "&cClear Occupation",
                    List.of("&7Click to clear your current occupation.", "&7Current: &f" + current.getDisplayName())));
        }

        player.openInventory(menu);
    }

    private static ItemStack summarizeOccupation(Occupation occupation, OccupationSettings settings, boolean selected) {
        List<String> lore = new ArrayList<>();
        for (String line : settings.description()) {
            lore.add("&7" + line);
        }
        lore.add(" ");
        switch (occupation) {
            case WARRIOR -> lore.add("&fHostile mobs: &a+" + settings.killHostileReward());
            case FARMER -> lore.add("&fHarvested crop: &a+" + settings.harvestReward());
            case FISHERMAN -> lore.add("&fCaught fish: &a+" + settings.catchReward());
            case LUMBERJACK -> lore.add("&fLog broken: &a+" + settings.logReward());
            case MINER -> lore.add("&fOre mined: &a+" + settings.oreReward());
            case EXPLORER -> lore.add("&fTravel: &a+" + settings.travelRewardPerBlock() + " per block");
            case BUILDER -> lore.add("&fNew block type: &a+" + settings.uniqueBlockReward());
        }
        lore.add(" ");
        if (selected) {
            lore.add("&aYou are currently this occupation.");
        } else {
            lore.add("&eClick to become a " + occupation.getDisplayName() + ".");
        }

        String name = (selected ? "&a" : "&6") + settings.displayName();
        ItemStack stack = item(settings.icon(), name, lore);
        if (selected) {
            stack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                stack.setItemMeta(meta);
            }
        }
        return stack;
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
