package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.views.AuctionHouseView;
import com.daytonjwatson.hardcore.utils.Util;

public final class BankTopGui {

    public static final String TITLE = Util.color("&6&lBank &8| &7Top Balances");

    private BankTopGui() {}

    public static void open(Player viewer) {
        open(viewer, 0);
    }

    public static void open(Player viewer, int page) {
        BankManager bank = BankManager.get();
        List<Map.Entry<java.util.UUID, Double>> entries = bank.getTopBalances(100);

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / 45.0));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory menu = Bukkit.createInventory(null, 54, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        int startIndex = currentPage * 45;
        int slot = 0;
        for (int i = startIndex; i < Math.min(entries.size(), startIndex + 45); i++) {
            Map.Entry<java.util.UUID, Double> entry = entries.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : "Unknown";
            ItemStack head = playerHead(player, Util.color("&e#" + (i + 1) + " &f" + name), List.of(
                    "&7Balance: &a" + bank.formatCurrency(entry.getValue()),
                    viewer.getUniqueId().equals(entry.getKey()) ? "&6You are ranked here!" : "&8Top account"
            ));
            menu.setItem(slot++, head);
        }

        if (entries.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, "&cNo accounts", List.of("&7Balances will appear here once saved."));
            menu.setItem(22, empty);
        }

        addNavigation(menu, currentPage, totalPages);
        viewer.openInventory(menu);
    }

    public static boolean isTopInventory(String title) {
        return ChatColor.stripColor(title).equals(ChatColor.stripColor(TITLE));
    }

    private static void addNavigation(Inventory menu, int currentPage, int totalPages) {
        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to the bank menu."));
        menu.setItem(45, back);

        ItemStack indicator = item(Material.NAME_TAG, "&ePage " + (currentPage + 1) + " of " + totalPages,
                List.of("&7Server-wide richest players."));
        menu.setItem(49, indicator);

        if (currentPage > 0) {
            ItemStack prev = navigationItem("&aPrevious Page", List.of("&7See earlier ranks."), currentPage - 1);
            menu.setItem(48, prev);
        }

        if (currentPage + 1 < totalPages) {
            ItemStack next = navigationItem("&aNext Page", List.of("&7See more wealthy players."), currentPage + 1);
            menu.setItem(50, next);
        }
    }

    public static ItemStack navigationItem(String name, List<String> lore, int targetPage) {
        ItemStack item = item(Material.ARROW, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(AuctionHouseView.navigationKey(),
                    org.bukkit.persistence.PersistentDataType.INTEGER, targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(Util.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Util.color(line));
            }
            meta.setLore(coloredLore);
            skull.setItemMeta(meta);
        }
        return skull;
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
            item.setItemMeta(meta);
        }
        return item;
    }
}
