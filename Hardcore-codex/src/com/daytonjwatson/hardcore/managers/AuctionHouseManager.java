package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.auction.AuctionListing;
import com.daytonjwatson.hardcore.utils.Util;

public class AuctionHouseManager {

    private static AuctionHouseManager instance;

    public static AuctionHouseManager init(HardcorePlugin plugin) {
        if (instance == null) {
            instance = new AuctionHouseManager(plugin);
        }
        return instance;
    }

    public static AuctionHouseManager get() {
        return instance;
    }

    private final HardcorePlugin plugin;
    private final File dataFile;
    private final FileConfiguration config;
    private final Map<UUID, AuctionListing> listings = new LinkedHashMap<>();
    private final Map<UUID, UUID> pendingPurchases = new ConcurrentHashMap<>();

    private AuctionHouseManager(HardcorePlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "auction-house.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        loadListings();
    }

    public List<AuctionListing> getListings() {
        return new ArrayList<>(listings.values());
    }

    public AuctionListing getListing(UUID id) {
        return listings.get(id);
    }

    public AuctionListing addListing(UUID seller, ItemStack item, double price, int quantity) {
        UUID id = UUID.randomUUID();
        AuctionListing listing = new AuctionListing(id, seller, item, price, quantity);
        listings.put(id, listing);
        saveListings();
        return listing;
    }

    public void removeListing(UUID id) {
        listings.remove(id);
        saveListings();
    }

    public void save() {
        saveListings();
    }

    public void setPendingPurchase(UUID player, UUID listing) {
        pendingPurchases.put(player, listing);
    }

    public UUID getPendingPurchase(UUID player) {
        return pendingPurchases.get(player);
    }

    public void clearPending(UUID player) {
        pendingPurchases.remove(player);
    }

    public boolean hasPending(UUID player) {
        return pendingPurchases.containsKey(player);
    }

    public boolean processPurchase(Player buyer, UUID listingId, int amount) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null) {
            buyer.sendMessage(Util.color("&cThat auction listing is no longer available."));
            return false;
        }

        if (amount <= 0 || amount > listing.getQuantity()) {
            buyer.sendMessage(Util.color("&cYou can buy between &f1 &cand &f" + listing.getQuantity() + "&c."));
            return false;
        }

        if (listing.getSeller() != null && listing.getSeller().equals(buyer.getUniqueId())) {
            buyer.sendMessage(Util.color("&cYou cannot purchase your own listing."));
            return false;
        }

        ItemStack item = listing.getItem();
        if (!canFit(buyer.getInventory(), item, amount)) {
            buyer.sendMessage(Util.color("&cYou don't have enough inventory space for that purchase."));
            return false;
        }

        BankManager bank = BankManager.get();
        double cost = listing.getPricePerItem() * amount;
        if (!bank.withdraw(buyer.getUniqueId(), cost, "Auction purchase: " + displayName(item) + " x" + amount)) {
            buyer.sendMessage(Util.color("&cYou don't have enough funds for that purchase."));
            return false;
        }

        listing.consume(amount);
        if (listing.getQuantity() <= 0) {
            listings.remove(listing.getId());
        }

        saveListings();

        UUID seller = listing.getSeller();
        if (seller != null) {
            bank.deposit(seller, cost, "Auction sale: " + displayName(item) + " x" + amount + " to " + buyer.getName());
        }

        giveItems(buyer, item, amount);
        buyer.sendMessage(Util.color("&aPurchased &f" + amount + "x &e" + displayName(item) + " &afor &f" + bank.formatCurrency(cost) + "&a."));
        return true;
    }

    private boolean canFit(Inventory inventory, ItemStack template, int amount) {
        int remaining = amount;
        ItemStack sample = template.clone();
        sample.setAmount(1);
        int maxStack = sample.getMaxStackSize();

        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                remaining -= maxStack;
            } else if (content.isSimilar(sample)) {
                remaining -= (maxStack - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private void giveItems(Player player, ItemStack template, int amount) {
        ItemStack base = template.clone();
        base.setAmount(1);
        int max = base.getMaxStackSize();

        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(max, remaining);
            ItemStack toGive = base.clone();
            toGive.setAmount(stack);
            player.getInventory().addItem(toGive);
            remaining -= stack;
        }
        player.updateInventory();
    }

    private String displayName(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        return Util.formatMaterialName(item.getType());
    }

    private void loadListings() {
        listings.clear();
        ConfigurationSection section = config.getConfigurationSection("listings");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                UUID seller = null;
                String sellerRaw = section.getString(key + ".seller");
                if (sellerRaw != null && !sellerRaw.isEmpty()) {
                    seller = UUID.fromString(sellerRaw);
                }
                double price = section.getDouble(key + ".price");
                int quantity = section.getInt(key + ".quantity");
                ItemStack item = section.getItemStack(key + ".item");
                if (item == null || quantity <= 0 || price <= 0) {
                    continue;
                }
                listings.put(id, new AuctionListing(id, seller, item, price, quantity));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveListings() {
        config.set("listings", null);
        Map<String, Object> serialized = new HashMap<>();
        for (AuctionListing listing : listings.values()) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("seller", listing.getSeller() == null ? null : listing.getSeller().toString());
            values.put("price", listing.getPricePerItem());
            values.put("quantity", listing.getQuantity());
            values.put("item", listing.getItem());
            serialized.put(listing.getId().toString(), values);
        }
        config.createSection("listings", serialized);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save auction listings: " + e.getMessage());
        }
    }
}
