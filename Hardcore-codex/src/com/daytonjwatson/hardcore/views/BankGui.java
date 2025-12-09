package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class BankGui {

    public static final String MAIN_TITLE = Util.color("&6&lBank &8| &7Account");
    public static final String HISTORY_TITLE = Util.color("&6&lBank &8| &7Transactions");

    private BankGui() {}

    public static void openMain(Player player) {
        BankManager bank = BankManager.get();
        Inventory menu = Bukkit.createInventory(null, 27, MAIN_TITLE);

        double balance = bank.getBalance(player.getUniqueId());

        ItemStack balanceItem = item(Material.EMERALD, "&aBalance", List.of(
                "&7Your current funds:",
                "&f" + bank.formatCurrency(balance)
        ));

        ItemStack sendItem = item(Material.GOLD_INGOT, "&eSend Money", List.of(
                "&7Send currency to another player.",
                "&7Click to choose a player and amount.",
                "&7You can still type &f/bank send <player> <amount>&7."
        ));

        ItemStack tradeItem = item(Material.CHEST_MINECART, "&6Trade Items", List.of(
                "&7Trade or gift items to online players.",
                "&7Charge a price that comes straight from their bank.",
                "&8Great for quick peer-to-peer deals."
        ));

        ItemStack topItem = item(Material.PAPER, "&bTop Balances", List.of(
                "&7See the richest players on the server.",
                "&7Click to open the baltop leaderboard.",
                "&8Updates as soon as balances change."
        ));

        List<String> recentLore = new ArrayList<>();
        recentLore.add("&7Most recent activity:");
        List<String> history = bank.getTransactions(player.getUniqueId());
        if (history.isEmpty()) {
            recentLore.add("&8No transactions yet.");
        } else {
            for (int i = 0; i < Math.min(5, history.size()); i++) {
                recentLore.add("&f" + history.get(i));
            }
        }
        recentLore.add("&8Click to view full history");
        ItemStack historyItem = item(Material.BOOK, "&bRecent Transactions", recentLore);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());

        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler);
        }

        menu.setItem(11, balanceItem);
        menu.setItem(13, sendItem);
        menu.setItem(15, historyItem);
        menu.setItem(20, tradeItem);
        menu.setItem(24, topItem);

        player.openInventory(menu);
    }

    public static void openTransactions(Player player) {
        BankManager bank = BankManager.get();
        List<String> history = bank.getTransactions(player.getUniqueId());

        int size = ((history.size() / 9) + 1) * 9;
        if (size < 27) size = 27;
        if (size > 54) size = 54;

        Inventory menu = Bukkit.createInventory(null, size, HISTORY_TITLE);

        int slot = 0;
        for (String entry : history) {
            if (slot >= size - 1) break; // leave room for back button
            ItemStack paper = item(Material.PAPER, "&fTransaction", List.of(entry));
            menu.setItem(slot++, paper);
        }

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to account overview"));
        menu.setItem(size - 1, back);

        player.openInventory(menu);
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
