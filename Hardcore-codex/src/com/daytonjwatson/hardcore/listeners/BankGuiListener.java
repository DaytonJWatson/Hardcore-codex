package com.daytonjwatson.hardcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.BankGui;

public class BankGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        String title = view.getTitle();

        if (!title.equals(BankGui.MAIN_TITLE) && !title.equals(BankGui.HISTORY_TITLE)) {
            return;
        }

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String rawName = current.getItemMeta() != null ? current.getItemMeta().getDisplayName() : "";
        String plainName = ChatColor.stripColor(rawName == null ? "" : rawName);

        if (title.equals(BankGui.MAIN_TITLE)) {
            if (plainName.equalsIgnoreCase("Send Money")) {
                player.closeInventory();
                player.sendMessage(Util.color("&6Bank &8» &7Use &f/bank send <player> <amount> &7to transfer funds."));
            } else if (plainName.equalsIgnoreCase("Recent Transactions")) {
                BankGui.openTransactions(player);
            }
            return;
        }

        if (title.equals(BankGui.HISTORY_TITLE) && plainName.equalsIgnoreCase("Back")) {
            BankGui.openMain(player);
        }
    }
}
