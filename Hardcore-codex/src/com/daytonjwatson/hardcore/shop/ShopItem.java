package com.daytonjwatson.hardcore.shop;

import org.bukkit.inventory.ItemStack;

public class ShopItem {

    private final ItemStack item;
    private double price;

    public ShopItem(ItemStack item, double price) {
        this.item = item.clone();
        this.price = price;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0, price);
    }
}
