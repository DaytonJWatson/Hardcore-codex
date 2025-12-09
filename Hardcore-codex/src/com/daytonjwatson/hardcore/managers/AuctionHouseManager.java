package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final long LISTING_DURATION_MILLIS = 48L * 60L * 60L * 1000L;

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
    private final Map<UUID, List<ItemStack>> pendingReturns = new HashMap<>();
    private final Map<UUID, List<String>> pendingMessages = new HashMap<>();

    private AuctionHouseManager(HardcorePlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "auction-house.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        loadListings();
        loadPendingReturns();
        loadPendingMessages();
        cleanupExpiredListings();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredListings, 20L * 60L, 20L * 60L);
    }

    public List<AuctionListing> getListings() {
        return new ArrayList<>(listings.values());
    }

    public AuctionListing getListing(UUID id) {
        return listings.get(id);
    }

    public AuctionListing addListing(UUID seller, ItemStack item, double price, int quantity) {
        UUID id = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + LISTING_DURATION_MILLIS;
        AuctionListing listing = new AuctionListing(id, seller, item, price, quantity, expiresAt);
        listings.put(id, listing);
        saveListings();
        return listing;
    }

    public int getListingCount(UUID seller) {
        if (seller == null) {
            return 0;
        }

        int count = 0;
        for (AuctionListing listing : listings.values()) {
            if (seller.equals(listing.getSeller())) {
                count++;
            }
        }
        return count;
    }

    public void removeListing(UUID id) {
        listings.remove(id);
        saveListings();
    }

    public boolean cancelListing(UUID id, String actor, String reason) {
        AuctionListing listing = listings.get(id);
        if (listing == null) {
            return false;
        }

        String itemName = displayName(listing.getItem());
        String message = Util.color("&6Auction &8» &cYour listing for &f" + listing.getQuantity() + "x &e"
                + itemName + " &cwas removed by &f" + actor + "&c. Reason: &7" + reason);
        returnListingToSeller(listing, message);
        saveListings();
        return true;
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

        if (listing.getExpiresAt() <= System.currentTimeMillis()) {
            buyer.sendMessage(Util.color("&cThat auction listing has expired."));
            listings.remove(listingId);
            saveListings();
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
            notifySellerSale(seller, displayName(item), amount, cost, buyer.getName());
        }

        giveItems(buyer, item, amount);
        buyer.sendMessage(Util.color("&aPurchased &f" + amount + "x &e" + displayName(item) + " &afor &f" + bank.formatCurrency(cost) + "&a."));
        return true;
    }

    public void deliverPending(Player player) {
        UUID playerId = player.getUniqueId();
        List<ItemStack> returns = pendingReturns.remove(playerId);
        if (returns != null && !returns.isEmpty()) {
            for (ItemStack item : returns) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            player.updateInventory();
            player.sendMessage(Util.color("&6Auction &8» &aYour expired auction items have been returned."));
        }

        List<String> messages = pendingMessages.remove(playerId);
        if (messages != null && !messages.isEmpty()) {
            for (String message : messages) {
                player.sendMessage(message);
            }
        }

        if ((returns != null && !returns.isEmpty()) || (messages != null && !messages.isEmpty())) {
            saveListings();
        }
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
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
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
                long expiresAt = section.getLong(key + ".expiresAt");
                if (expiresAt <= 0L) {
                    expiresAt = System.currentTimeMillis() + LISTING_DURATION_MILLIS;
                }
                if (item == null || quantity <= 0 || price <= 0) {
                    continue;
                }
                listings.put(id, new AuctionListing(id, seller, item, price, quantity, expiresAt));
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
            values.put("expiresAt", listing.getExpiresAt());
            serialized.put(listing.getId().toString(), values);
        }
        config.createSection("listings", serialized);

        config.set("pendingReturns", null);
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingReturns.entrySet()) {
            List<ItemStack> toSave = new ArrayList<>();
            for (ItemStack item : entry.getValue()) {
                toSave.add(item.clone());
            }
            config.set("pendingReturns." + entry.getKey(), toSave);
        }

        config.set("pendingMessages", null);
        for (Map.Entry<UUID, List<String>> entry : pendingMessages.entrySet()) {
            config.set("pendingMessages." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save auction listings: " + e.getMessage());
        }
    }

    private void notifySellerSale(UUID sellerId, String itemName, int amount, double revenue, String buyerName) {
        String message = Util.color("&6Auction &8» &aYour listing for &f" + amount + "x &e" + itemName
                + " &awas purchased by &f" + buyerName + " &afor &f" + BankManager.get().formatCurrency(revenue) + "&a.");
        Player seller = plugin.getServer().getPlayer(sellerId);
        if (seller != null && seller.isOnline()) {
            seller.sendMessage(message);
            return;
        }

        pendingMessages.computeIfAbsent(sellerId, id -> new ArrayList<>()).add(message);
        saveListings();
    }

    private void returnListingToSeller(AuctionListing listing, String message) {
        UUID sellerId = listing.getSeller();
        if (sellerId == null) {
            listings.remove(listing.getId());
            return;
        }

        List<ItemStack> stacks = splitStacks(listing.getItem(), listing.getQuantity());
        Player seller = plugin.getServer().getPlayer(sellerId);
        if (seller != null && seller.isOnline()) {
            for (ItemStack stack : stacks) {
                Map<Integer, ItemStack> leftovers = seller.getInventory().addItem(stack);
                for (ItemStack leftover : leftovers.values()) {
                    seller.getWorld().dropItemNaturally(seller.getLocation(), leftover);
                }
            }
            seller.updateInventory();
            if (message != null) {
                seller.sendMessage(message);
            }
        } else {
            pendingReturns.computeIfAbsent(sellerId, id -> new ArrayList<>()).addAll(stacks);
            if (message != null) {
                pendingMessages.computeIfAbsent(sellerId, id -> new ArrayList<>()).add(message);
            }
        }

        listings.remove(listing.getId());
    }

    private void handleExpiry(AuctionListing listing) {
        String message = Util.color("&6Auction &8» &cYour listing for &f" + listing.getQuantity() + "x &e"
                + displayName(listing.getItem()) + " &chas expired and was returned to you.");
        returnListingToSeller(listing, message);
    }

    private List<ItemStack> splitStacks(ItemStack template, int quantity) {
        if (quantity <= 0) {
            return Collections.emptyList();
        }

        ItemStack base = template.clone();
        base.setAmount(1);
        int maxStack = base.getMaxStackSize();
        List<ItemStack> stacks = new ArrayList<>();

        int remaining = quantity;
        while (remaining > 0) {
            int stackAmount = Math.min(maxStack, remaining);
            ItemStack stack = base.clone();
            stack.setAmount(stackAmount);
            stacks.add(stack);
            remaining -= stackAmount;
        }
        return stacks;
    }

    private void cleanupExpiredListings() {
        long now = System.currentTimeMillis();
        List<AuctionListing> expired = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            if (listing.getExpiresAt() <= now) {
                expired.add(listing);
            }
        }

        if (expired.isEmpty()) {
            return;
        }

        for (AuctionListing listing : expired) {
            handleExpiry(listing);
        }
        saveListings();
    }

    private void loadPendingReturns() {
        ConfigurationSection section = config.getConfigurationSection("pendingReturns");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                List<ItemStack> items = new ArrayList<>();
                for (Object obj : section.getList(key, List.of())) {
                    if (obj instanceof ItemStack stack) {
                        items.add(stack.clone());
                    }
                }
                if (!items.isEmpty()) {
                    pendingReturns.put(playerId, items);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadPendingMessages() {
        ConfigurationSection section = config.getConfigurationSection("pendingMessages");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                List<String> messages = new ArrayList<>();
                for (Object obj : section.getList(key, List.of())) {
                    if (obj instanceof String message) {
                        messages.add(message);
                    }
                }
                if (!messages.isEmpty()) {
                    pendingMessages.put(playerId, messages);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
