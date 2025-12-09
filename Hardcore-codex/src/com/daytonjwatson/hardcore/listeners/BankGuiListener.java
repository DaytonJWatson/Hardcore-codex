package com.daytonjwatson.hardcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.BankGui;
import com.daytonjwatson.hardcore.views.BankSendGui;

public class BankGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        String title = view.getTitle();

        if (!title.equals(BankGui.MAIN_TITLE) && !title.equals(BankGui.HISTORY_TITLE)
                && !BankSendGui.isSendInventory(title)) {
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
                BankSendGui.openRecipientSelection(player);
            } else if (plainName.equalsIgnoreCase("Recent Transactions")) {
                BankGui.openTransactions(player);
            }
            return;
        }

        if (title.equals(BankGui.HISTORY_TITLE) && plainName.equalsIgnoreCase("Back")) {
            BankGui.openMain(player);
            return;
        }

        if (title.equals(BankSendGui.SELECT_TITLE)) {
            if (plainName.equalsIgnoreCase("Back")) {
                BankGui.openMain(player);
                return;
            }

            java.util.UUID uuid = BankSendGui.getTargetFromItem(current);
            if (uuid == null) {
                return;
            }

            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            BankSendGui.openAmountSelection(player, target);
            return;
        }

        if (BankSendGui.isSendInventory(title) && plainName.equalsIgnoreCase("Choose different player")) {
            BankSendGui.openRecipientSelection(player);
            return;
        }

        if (BankSendGui.isSendInventory(title) && plainName.equalsIgnoreCase("Enter Custom Amount")) {
            player.closeInventory();
            player.sendMessage(Util.color("&6Bank &8» &7Type &f/bank send <player> <amount> &7to send via chat."));
            return;
        }

        if (BankSendGui.isSendInventory(title)) {
            java.util.UUID targetId = BankSendGui.getTargetFromItem(current);
            Double amount = BankSendGui.getAmountFromItem(current);
            String multiplierRaw = BankSendGui.getMultiplierData(current);

            if (targetId == null || (amount == null && multiplierRaw == null)) {
                return;
            }

            double finalAmount = amount != null ? amount : 0.0;
            if (multiplierRaw != null) {
                try {
                    double multiplier = Double.parseDouble(multiplierRaw);
                    finalAmount = BankManager.get().getBalance(player.getUniqueId()) * multiplier;
                } catch (NumberFormatException ignored) {
                    return;
                }
            }

            if (finalAmount <= 0) {
                player.sendMessage(Util.color("&cInvalid amount."));
                return;
            }

            BankManager bank = BankManager.get();
            if (!bank.transfer(player.getUniqueId(), targetId, finalAmount)) {
                player.sendMessage(Util.color("&cYou don't have enough funds to send that amount."));
                return;
            }

            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(targetId);
            String targetName = target.getName() != null ? target.getName() : "player";
            player.sendMessage(Util.color("&aSent &f" + bank.formatCurrency(finalAmount) + " &ato &e" + targetName + "&a."));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Util.color("&aYou received &f" + bank.formatCurrency(finalAmount) + " &afrom &e"
                        + player.getName() + "&a."));
            }
            player.closeInventory();
        }
    }
}
