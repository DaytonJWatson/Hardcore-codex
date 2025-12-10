package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.shop.ShopItem;
import com.daytonjwatson.hardcore.utils.Util;

public class ShopManager {

    private static ShopManager instance;

    public static ShopManager init(HardcorePlugin plugin) {
        if (instance == null) {
            instance = new ShopManager(plugin);
        }
        return instance;
    }

    public static ShopManager get() {
        return instance;
    }

    private final HardcorePlugin plugin;
    private final File dataFile;
    private final FileConfiguration data;
    private final Map<UUID, PlayerShop> shops = new LinkedHashMap<>();
    private final Map<UUID, UUID> pendingPurchase = new HashMap<>();
    private final Map<UUID, UUID> pendingRename = new HashMap<>();
    private final Map<UUID, UUID> pendingDescription = new HashMap<>();
    private final Map<UUID, PendingItem> pendingItemAdds = new HashMap<>();
    private final Map<UUID, UUID> pendingPriceUpdate = new HashMap<>();
    private static final UUID LEGACY_SERVER_OWNER = UUID.nameUUIDFromBytes("hardcore-server-shop".getBytes());

    private ShopManager(HardcorePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "shops.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    public List<PlayerShop> getShops() {
        return new ArrayList<>(shops.values());
    }

    public PlayerShop getShop(UUID id) {
        return shops.get(id);
    }

    public List<PlayerShop> getShopsOwnedBy(UUID owner) {
        List<PlayerShop> owned = new ArrayList<>();
        for (PlayerShop shop : shops.values()) {
            if (shop.getOwner().equals(owner)) {
                owned.add(shop);
            }
        }
        return owned;
    }

    public boolean canCreateShop(UUID owner) {
        return getShopsOwnedBy(owner).size() < ConfigValues.maxShopsPerPlayer();
    }

    public PlayerShop createShop(Player player) {
        if (!canCreateShop(player.getUniqueId())) {
            player.sendMessage(Util.color("&cYou have reached the shop limit."));
            return null;
        }

        BankManager bank = BankManager.get();
        double cost = ConfigValues.shopCreationCost();
        if (!bank.withdraw(player.getUniqueId(), cost, "Shop creation")) {
            player.sendMessage(Util.color("&cYou need " + bank.formatCurrency(cost) + " to open a shop."));
            return null;
        }

        UUID id = UUID.randomUUID();
        PlayerShop shop = new PlayerShop(id, player.getUniqueId(), Util.color(player.getName() + "'s Shop"),
                Util.color("Browse my wares!"), new ItemStack(Material.CHEST), true);
        shops.put(id, shop);
        save();
        player.sendMessage(Util.color("&aShop created! Use the manager to customize it."));
        return shop;
    }

    public void deleteShop(UUID id) {
        shops.remove(id);
        save();
    }

    public void save() {
        data.set("shops", null);
        for (PlayerShop shop : shops.values()) {
            String base = "shops." + shop.getId() + ".";
            data.set(base + "owner", shop.getOwner().toString());
            data.set(base + "name", shop.getName());
            data.set(base + "description", shop.getDescription());
            data.set(base + "open", shop.isOpen());
            data.set(base + "icon", shop.getIcon());

            Map<Integer, ShopItem> stock = shop.getStock();
            for (Map.Entry<Integer, ShopItem> entry : stock.entrySet()) {
                ShopItem item = entry.getValue();
                data.set(base + "items." + entry.getKey() + ".item", item.getItem());
                data.set(base + "items." + entry.getKey() + ".price", item.getPrice());
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save shops: " + e.getMessage());
        }
    }

    private void load() {
        shops.clear();
        ConfigurationSection section = data.getConfigurationSection("shops");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                UUID owner = UUID.fromString(section.getString(key + ".owner"));
                if (LEGACY_SERVER_OWNER.equals(owner)) {
                    continue;
                }
                String name = section.getString(key + ".name", "Shop");
                String description = section.getString(key + ".description", "Wares");
                boolean open = section.getBoolean(key + ".open", true);
                ItemStack icon = section.getItemStack(key + ".icon", new ItemStack(Material.CHEST));

                PlayerShop shop = new PlayerShop(id, owner, name, description, icon, open);
                ConfigurationSection items = section.getConfigurationSection(key + ".items");
                if (items != null) {
                    for (String slotKey : items.getKeys(false)) {
                        int slot = Integer.parseInt(slotKey);
                        ItemStack itemStack = items.getItemStack(slotKey + ".item");
                        double price = items.getDouble(slotKey + ".price", 0.0);
                        if (itemStack != null && itemStack.getType() != Material.AIR) {
                            shop.setItem(slot, new ShopItem(itemStack, price));
                        }
                    }
                }

                shops.put(id, shop);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void setPendingPurchase(UUID player, UUID shop) {
        pendingPurchase.put(player, shop);
    }

    public boolean hasPendingPurchase(UUID player) {
        return pendingPurchase.containsKey(player);
    }

    public void clearPendingPurchase(UUID player) {
        pendingPurchase.remove(player);
    }

    public UUID getPendingShop(UUID player) {
        return pendingPurchase.get(player);
    }

    public void setPendingRename(UUID player, UUID shop) {
        pendingRename.put(player, shop);
    }

    public void setPendingDescription(UUID player, UUID shop) {
        pendingDescription.put(player, shop);
    }

    public boolean isRenaming(UUID player) {
        return pendingRename.containsKey(player);
    }

    public boolean isDescribing(UUID player) {
        return pendingDescription.containsKey(player);
    }

    public PlayerShop consumeRenameTarget(UUID player) {
        UUID id = pendingRename.remove(player);
        return id == null ? null : getShop(id);
    }

    public PlayerShop consumeDescriptionTarget(UUID player) {
        UUID id = pendingDescription.remove(player);
        return id == null ? null : getShop(id);
    }

    public void setPendingPrice(UUID player, UUID shop) {
        pendingPriceUpdate.put(player, shop);
    }

    public PlayerShop consumePriceTarget(UUID player) {
        UUID id = pendingPriceUpdate.remove(player);
        return id == null ? null : getShop(id);
    }

    public boolean isAwaitingPrice(UUID player) {
        return pendingPriceUpdate.containsKey(player);
    }

    public UUID peekPriceTarget(UUID player) {
        return pendingPriceUpdate.get(player);
    }

    public void setPendingItemAdd(UUID player, UUID shop, ItemStack item) {
        pendingItemAdds.put(player, new PendingItem(shop, item));
    }

    public PendingItem consumePendingItem(UUID player) {
        return pendingItemAdds.remove(player);
    }

    public record PendingItem(UUID shopId, ItemStack item) {}

    public boolean processPurchase(Player buyer, UUID shopId, int slot) {
        PlayerShop shop = shops.get(shopId);
        if (shop == null || !shop.isOpen()) {
            buyer.sendMessage(Util.color("&cThat shop is no longer available."));
            return false;
        }
        ShopItem listing = shop.getStock().get(slot);
        if (listing == null) {
            buyer.sendMessage(Util.color("&cThat item is gone."));
            return false;
        }

        ItemStack item = listing.getItem();
        double price = listing.getPrice();
        BankManager bank = BankManager.get();
        if (!bank.withdraw(buyer.getUniqueId(), price, "Purchase from shop: " + shop.getName())) {
            buyer.sendMessage(Util.color("&cYou cannot afford that. Cost: " + bank.formatCurrency(price)));
            return false;
        }

        bank.deposit(shop.getOwner(), price, "Sale in shop: " + buyer.getName());
        buyer.getInventory().addItem(item);

        shop.getStock().remove(slot);
        save();
        buyer.sendMessage(Util.color("&aPurchased &f" + item.getAmount() + "x &e" + Util.plainName(item)
                + " &afor &f" + bank.formatCurrency(price) + "&a."));
        return true;
    }

    public boolean addItemToShop(PlayerShop shop, ItemStack item, double price) {
        Map<Integer, ShopItem> stock = shop.getStock();
        Set<Integer> slots = stock.keySet();
        int slot = 0;
        while (slot < 27 && slots.contains(slot)) {
            slot++;
        }
        if (slot >= 27) {
            return false;
        }

        stock.put(slot, new ShopItem(item, price));
        save();
        return true;
    }

    public List<PlayerShop> getOpenShops() {
        List<PlayerShop> open = new ArrayList<>();
        for (PlayerShop shop : shops.values()) {
            if (shop.isOpen()) {
                open.add(shop);
            }
        }
        return open;
    }

    public List<PlayerShop> getSortedShops() {
        return Collections.unmodifiableList(new ArrayList<>(shops.values()));
    }
}
