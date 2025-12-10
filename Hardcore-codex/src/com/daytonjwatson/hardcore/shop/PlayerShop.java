package com.daytonjwatson.hardcore.shop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.utils.Util;

public class PlayerShop {

    private final UUID id;
    private final UUID owner;
    private String name;
    private String description;
    private ItemStack icon;
    private boolean open;
    private final Map<Integer, ShopItem> stock = new LinkedHashMap<>();

    public PlayerShop(UUID id, UUID owner, String name, String description, ItemStack icon, boolean open) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.icon = icon == null ? new ItemStack(Material.CHEST) : icon.clone();
        this.open = open;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Util.color(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Util.color(description);
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public void setIcon(ItemStack icon) {
        if (icon != null && icon.getType() != Material.AIR) {
            this.icon = icon.clone();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Map<Integer, ShopItem> getStock() {
        return stock;
    }

    public void clearStock() {
        stock.clear();
    }

    public void setItem(int slot, ShopItem item) {
        if (slot < 0 || slot >= 27) {
            return;
        }
        if (item == null) {
            stock.remove(slot);
            return;
        }
        stock.put(slot, item);
    }

    public OfflinePlayer getOwnerPlayer() {
        return org.bukkit.Bukkit.getOfflinePlayer(owner);
    }
}
