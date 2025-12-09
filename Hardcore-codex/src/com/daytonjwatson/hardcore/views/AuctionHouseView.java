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
    private static final NamespacedKey NAV_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "auction_nav_target");
    private static final int PAGE_SIZE = 45;

    private AuctionHouseView() {}

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        AuctionHouseManager manager = AuctionHouseManager.get();
        List<AuctionListing> listings = manager.getListings();
        int totalPages = Math.max(1, (int) Math.ceil(listings.size() / (double) PAGE_SIZE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory menu = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler);
        }

        int slot = 0;
        BankManager bank = BankManager.get();
        int startIndex = currentPage * PAGE_SIZE;
        for (int i = startIndex; i < Math.min(listings.size(), startIndex + PAGE_SIZE); i++) {
            AuctionListing listing = listings.get(i);
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
            menu.setItem(22, empty);
        }

        addNavigation(menu, currentPage, totalPages);

        player.openInventory(menu);
    }

    public static boolean isAuctionInventory(String title) {
        return ChatColor.stripColor(title).equals(ChatColor.stripColor(TITLE));
    }

    public static NamespacedKey listingKey() {
        return LISTING_KEY;
    }

    public static NamespacedKey navigationKey() {
        return NAV_KEY;
    }

    public static int pageSize() {
        return PAGE_SIZE;
    }

    public static ItemStack navigationItem(String name, List<String> lore, int targetPage) {
        ItemStack item = item(Material.ARROW, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(NAV_KEY, PersistentDataType.INTEGER, targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void addNavigation(Inventory menu, int currentPage, int totalPages) {
        ItemStack close = item(Material.BARRIER, "&cClose", List.of("&7Click to close the auction house."));
        menu.setItem(49, close);

        ItemStack indicator = item(Material.NAME_TAG, "&ePage " + (currentPage + 1) + " of " + totalPages,
                List.of("&7Browse all available listings."));
        menu.setItem(50, indicator);

        if (currentPage > 0) {
            menu.setItem(45, navigationItem("&aPrevious Page", List.of("&7View earlier listings."), currentPage - 1));
        }

        if (currentPage + 1 < totalPages) {
            menu.setItem(53, navigationItem("&aNext Page", List.of("&7View more listings."), currentPage + 1));
        }
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
