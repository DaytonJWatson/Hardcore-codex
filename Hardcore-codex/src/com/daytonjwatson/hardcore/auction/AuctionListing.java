package com.daytonjwatson.hardcore.auction;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class AuctionListing {

    private final UUID id;
    private final UUID seller;
    private final ItemStack item;
    private final double pricePerItem;
    private final long expiresAt;
    private int quantity;

    public AuctionListing(UUID id, UUID seller, ItemStack item, double pricePerItem, int quantity, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.item = item.clone();
        this.item.setAmount(1);
        this.pricePerItem = pricePerItem;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getPricePerItem() {
        return pricePerItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void consume(int amount) {
        quantity = Math.max(0, quantity - amount);
    }
}
