package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.auction.AuctionListing;
import com.daytonjwatson.hardcore.managers.AuctionHouseManager;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;

public final class AuctionHouseView {

    public static final String TITLE = Util.color("&6&lAuction House");
    private static final NamespacedKey LISTING_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "auction_listing_id");

    private AuctionHouseView() {}

    public static void open(Player player) {
        AuctionHouseManager manager = AuctionHouseManager.get();
        List<AuctionListing> listings = manager.getListings();
        int size = Math.min(54, Math.max(27, ((listings.size() / 9) + 1) * 9));
        Inventory menu = Bukkit.createInventory(null, size, TITLE);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < size; slot++) {
            menu.setItem(slot, filler);
        }

        int slot = 0;
        BankManager bank = BankManager.get();
        for (AuctionListing listing : listings) {
            if (slot >= size) break;
            ItemStack display = listing.getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Util.color("&7Price: &f" + bank.formatCurrency(listing.getPricePerItem())));
                lore.add(Util.color("&7Available: &f" + listing.getQuantity()));
                lore.add(Util.color("&8Left-click to buy. You'll be asked for an amount."));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(LISTING_KEY, PersistentDataType.STRING,
                        listing.getId().toString());
                display.setItemMeta(meta);
            }
            display.setAmount(Math.max(1, Math.min(display.getMaxStackSize(), listing.getQuantity())));
            menu.setItem(slot++, display);
        }

        if (listings.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, "&cNo listings", List.of("&7No items are for sale right now."));
            menu.setItem(size / 2, empty);
        }

        player.openInventory(menu);
    }

    public static boolean isAuctionInventory(String title) {
        return ChatColor.stripColor(title).equals(ChatColor.stripColor(TITLE));
    }

    public static NamespacedKey listingKey() {
        return LISTING_KEY;
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
