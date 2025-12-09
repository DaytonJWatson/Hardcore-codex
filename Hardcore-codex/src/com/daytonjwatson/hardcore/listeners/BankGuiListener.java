package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.managers.BankTradeManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.BankGui;
import com.daytonjwatson.hardcore.views.BankSendGui;
import com.daytonjwatson.hardcore.views.BankTopGui;
import com.daytonjwatson.hardcore.views.BankTradeGui;

public class BankGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        String title = view.getTitle();

        if (!title.equals(BankGui.MAIN_TITLE) && !title.equals(BankGui.HISTORY_TITLE)
                && !BankSendGui.isSendInventory(title)
                && !BankTopGui.isTopInventory(title)
                && !BankTradeGui.isTradeInventory(title)) {
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
                BankSendGui.openRecipientSelection(player, 0);
            } else if (plainName.equalsIgnoreCase("Recent Transactions")) {
                BankGui.openTransactions(player);
            } else if (plainName.equalsIgnoreCase("Trade Items")) {
                BankTradeGui.openRecipientSelection(player, 0);
            } else if (plainName.equalsIgnoreCase("Top Balances")) {
                BankTopGui.open(player, 0);
            }
            return;
        }

        if (title.equals(BankGui.HISTORY_TITLE) && plainName.equalsIgnoreCase("Back")) {
            BankGui.openMain(player);
            return;
        }

        if (BankTopGui.isTopInventory(title)) {
            if (plainName.equalsIgnoreCase("Back")) {
                BankGui.openMain(player);
                return;
            }

            Integer navPage = null;
            if (current.getItemMeta() != null && current.getItemMeta().getPersistentDataContainer()
                    .has(com.daytonjwatson.hardcore.views.AuctionHouseView.navigationKey(),
                            org.bukkit.persistence.PersistentDataType.INTEGER)) {
                navPage = current.getItemMeta().getPersistentDataContainer().get(
                        com.daytonjwatson.hardcore.views.AuctionHouseView.navigationKey(),
                        org.bukkit.persistence.PersistentDataType.INTEGER);
            }
            if (navPage != null) {
                BankTopGui.open(player, navPage);
            }
            return;
        }

        if (title.equals(BankSendGui.SELECT_TITLE)) {
            Integer navPage = BankSendGui.getPageFromItem(current);
            if (navPage != null && BankSendGui.getTargetFromItem(current) == null && plainName.toLowerCase().contains("page")) {
                BankSendGui.openRecipientSelection(player, navPage);
                return;
            }

            if (plainName.equalsIgnoreCase("Back")) {
                BankGui.openMain(player);
                return;
            }

            java.util.UUID uuid = BankSendGui.getTargetFromItem(current);
            if (uuid == null) {
                return;
            }

            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            int currentPage = navPage == null ? 0 : navPage;
            BankSendGui.openAmountSelection(player, target, currentPage);
            return;
        }

        if (BankSendGui.isSendInventory(title) && plainName.equalsIgnoreCase("Choose different player")) {
            Integer page = BankSendGui.getPageFromItem(current);
            BankSendGui.openRecipientSelection(player, page == null ? 0 : page);
            return;
        }

        if (BankSendGui.isSendInventory(title) && plainName.equalsIgnoreCase("Enter Custom Amount")) {
            java.util.UUID targetId = BankSendGui.getTargetFromItem(current);
            if (targetId == null) {
                player.sendMessage(Util.color("&cCould not determine who you're sending to. Please pick a player again."));
                Integer page = BankSendGui.getPageFromItem(current);
                BankSendGui.openRecipientSelection(player, page == null ? 0 : page);
                return;
            }

            BankManager.get().setPendingCustomSend(player.getUniqueId(), targetId);
            player.closeInventory();
            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(targetId);
            String targetName = target.getName() != null ? target.getName() : "that player";
            player.sendMessage(Util.color("&6Bank &8» &7Type the amount to send to &e" + targetName
                    + " &7in chat, or type &ccancel &7to abort."));
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

        if (title.equals(BankTradeGui.SELECT_TITLE)) {
            Integer navPage = BankTradeGui.getPageFromItem(current);
            if (navPage != null && BankTradeGui.getTargetFromItem(current) == null && plainName.toLowerCase().contains("page")) {
                BankTradeGui.openRecipientSelection(player, navPage);
                return;
            }

            if (plainName.equalsIgnoreCase("Back")) {
                BankTradeManager.get().clear(player.getUniqueId());
                BankGui.openMain(player);
                return;
            }

            java.util.UUID targetId = BankTradeGui.getTargetFromItem(current);
            if (targetId == null) {
                return;
            }

            Player target = org.bukkit.Bukkit.getPlayer(targetId);
            if (target == null) {
                player.sendMessage(Util.color("&cThat player is no longer online."));
                BankTradeGui.openRecipientSelection(player, navPage == null ? 0 : navPage);
                return;
            }
            BankTradeGui.openOfferConfirm(player, target, navPage == null ? 0 : navPage);
            return;
        }

        if (BankTradeGui.isTradeInventory(title) && plainName.equalsIgnoreCase("Choose different player")) {
            Integer page = BankTradeGui.getPageFromItem(current);
            BankTradeManager.get().clear(player.getUniqueId());
            BankTradeGui.openRecipientSelection(player, page == null ? 0 : page);
            return;
        }

        if (BankTradeGui.isTradeInventory(title) && plainName.equalsIgnoreCase("Enter Custom Price")) {
            java.util.UUID targetId = BankTradeGui.getTargetFromItem(current);
            if (targetId == null) {
                player.sendMessage(Util.color("&cCould not determine who you're trading with. Please pick a player again."));
                Integer page = BankTradeGui.getPageFromItem(current);
                BankTradeGui.openRecipientSelection(player, page == null ? 0 : page);
                return;
            }

            BankTradeManager.get().setAwaitingPrice(player.getUniqueId(), true);
            player.closeInventory();
            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(targetId);
            String targetName = target.getName() != null ? target.getName() : "that player";
            player.sendMessage(Util.color("&6Bank &8» &7Type the price for your trade to &e" + targetName
                    + " &7in chat, or type &ccancel &7to abort."));
            return;
        }

        if (BankTradeGui.isTradeInventory(title)) {
            java.util.UUID targetId = BankTradeGui.getTargetFromItem(current);
            Double price = BankTradeGui.getPriceFromItem(current);

            if (targetId == null || price == null) {
                return;
            }

            Player target = org.bukkit.Bukkit.getPlayer(targetId);
            if (target == null) {
                player.sendMessage(Util.color("&cThat player went offline."));
                BankTradeGui.openRecipientSelection(player, BankTradeGui.getPageFromItem(current) == null ? 0 : BankTradeGui.getPageFromItem(current));
                return;
            }

            if (BankTradeManager.get().executeTrade(player, target, price)) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BankManager bank = BankManager.get();
        if (!bank.hasPendingCustomSend(player.getUniqueId()) && !BankTradeManager.get().isAwaitingPrice(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = ChatColor.stripColor(event.getMessage().trim());
        if (message.equalsIgnoreCase("cancel")) {
            if (bank.hasPendingCustomSend(player.getUniqueId())) {
                bank.clearPendingCustomSend(player.getUniqueId());
                player.sendMessage(Util.color("&6Bank &8» &7Transfer cancelled."));
            }
            if (BankTradeManager.get().isAwaitingPrice(player.getUniqueId())) {
                BankTradeManager.get().setAwaitingPrice(player.getUniqueId(), false);
                BankTradeManager.get().clear(player.getUniqueId());
                player.sendMessage(Util.color("&6Bank &8» &7Trade cancelled."));
            }
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(message);
        } catch (NumberFormatException ex) {
            player.sendMessage(Util.color("&cPlease enter a numeric amount or type 'cancel'."));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Util.color("&cAmount must be positive. Type another amount or 'cancel'."));
            return;
        }

        if (bank.hasPendingCustomSend(player.getUniqueId())) {
            java.util.UUID targetId = bank.getPendingCustomSend(player.getUniqueId());
            bank.clearPendingCustomSend(player.getUniqueId());

            Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(), () -> {
                if (targetId == null) {
                    player.sendMessage(Util.color("&cCould not find who to send that to."));
                    return;
                }

                if (targetId.equals(player.getUniqueId())) {
                    player.sendMessage(Util.color("&cYou cannot send money to yourself."));
                    return;
                }

                com.daytonjwatson.hardcore.managers.BankManager manager = com.daytonjwatson.hardcore.managers.BankManager.get();
                if (!manager.transfer(player.getUniqueId(), targetId, amount)) {
                    player.sendMessage(Util.color("&cYou don't have enough funds to send that amount."));
                    return;
                }

                org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(targetId);
                String targetName = target.getName() != null ? target.getName() : "player";
                player.sendMessage(Util.color("&aSent &f" + manager.formatCurrency(amount) + " &ato &e" + targetName + "&a."));
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(Util.color("&aYou received &f" + manager.formatCurrency(amount) + " &afrom &e"
                            + player.getName() + "&a."));
                }
            });
            return;
        }

        if (BankTradeManager.get().isAwaitingPrice(player.getUniqueId())) {
            BankTradeManager.get().setAwaitingPrice(player.getUniqueId(), false);
            BankTradeManager.TradeSession session = BankTradeManager.get().getPendingTrade(player.getUniqueId());
            if (session == null) {
                player.sendMessage(Util.color("&cTrade data expired. Please open the trade menu again."));
                return;
            }

            Player target = org.bukkit.Bukkit.getPlayer(session.target());
            if (target == null) {
                player.sendMessage(Util.color("&cThat player went offline."));
                BankTradeManager.get().clear(player.getUniqueId());
                return;
            }

            double finalAmount = amount;
            Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(), () -> {
                if (BankTradeManager.get().executeTrade(player, target, finalAmount)) {
                    player.closeInventory();
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BankManager.get().clearPendingCustomSend(event.getPlayer().getUniqueId());
        BankTradeManager.get().clear(event.getPlayer().getUniqueId());
    }
}
