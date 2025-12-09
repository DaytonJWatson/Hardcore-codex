package com.daytonjwatson.hardcore.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.auction.AuctionListing;
import com.daytonjwatson.hardcore.managers.AuctionHouseManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.AuctionHouseView;

public class AuctionHouseListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        String title = view.getTitle();
        if (!AuctionHouseView.isAuctionInventory(title)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) {
            return;
        }

        Integer targetPage = clicked.getItemMeta().getPersistentDataContainer()
                .get(AuctionHouseView.navigationKey(), PersistentDataType.INTEGER);
        if (targetPage != null) {
            AuctionHouseView.open(player, targetPage);
            return;
        }

        String name = clicked.getItemMeta().getDisplayName();
        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Close")) {
            player.closeInventory();
            return;
        }

        String idRaw = clicked.getItemMeta().getPersistentDataContainer()
                .get(AuctionHouseView.listingKey(), PersistentDataType.STRING);
        if (idRaw == null) {
            return;
        }

        AuctionHouseManager manager = AuctionHouseManager.get();
        AuctionListing match;
        try {
            match = manager.getListing(UUID.fromString(idRaw));
        } catch (IllegalArgumentException ex) {
            return;
        }

        if (match == null) {
            player.sendMessage(Util.color("&cThat listing is no longer available."));
            return;
        }

        manager.setPendingPurchase(player.getUniqueId(), match.getId());
        player.closeInventory();
        player.sendMessage(Util.color("&6Auction &8» &7Enter how many you want to buy (1-" + match.getQuantity() + ") in chat, or type &ccancel &7to abort."));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        AuctionHouseManager manager = AuctionHouseManager.get();
        if (!manager.hasPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = ChatColor.stripColor(event.getMessage().trim());
        if (message.equalsIgnoreCase("cancel")) {
            manager.clearPending(player.getUniqueId());
            player.sendMessage(Util.color("&6Auction &8» &7Purchase cancelled."));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException ex) {
            player.sendMessage(Util.color("&cPlease enter a whole number amount or type 'cancel'."));
            return;
        }

        UUID listingId = manager.getPendingPurchase(player.getUniqueId());
        manager.clearPending(player.getUniqueId());

        Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(), () -> manager.processPurchase(player, listingId, amount));
    }
}
