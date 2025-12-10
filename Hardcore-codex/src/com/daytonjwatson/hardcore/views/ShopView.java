package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.shop.ShopItem;
import com.daytonjwatson.hardcore.utils.Util;

public final class ShopView {

    public static final String TITLE_PREFIX = Util.color("&6&lShop &8| &7");
    private static final NamespacedKey SHOP_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_view_id");
    private static final NamespacedKey ITEM_SLOT = new NamespacedKey(HardcorePlugin.getInstance(), "shop_view_slot");

    private ShopView() {}

    public static void open(Player viewer, PlayerShop shop) {
        Inventory menu = Bukkit.createInventory(null, 54, TITLE_PREFIX + ChatColor.stripColor(shop.getName()));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        int slot = 0;
        BankManager bank = BankManager.get();
        for (int i = 0; i < 27; i++) {
            ShopItem entry = shop.getStock().get(i);
            if (entry == null) continue;
            ItemStack display = entry.getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Util.color("&7Price: &f" + bank.formatCurrency(entry.getPrice())));
                lore.add(Util.color("&8Left-click to purchase."));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
                meta.getPersistentDataContainer().set(ITEM_SLOT, PersistentDataType.INTEGER, i);
                display.setItemMeta(meta);
            }
            menu.setItem(slot++, display);
        }

        ItemStack info = item(Material.BOOK, "&eShop Info", List.of(
                "&7Owner: &f" + Util.resolveShopOwnerName(shop),
                "&7Listings: &f" + shop.getStock().size(),
                "&7Status: " + (shop.isOpen() ? "&aOpen" : "&cClosed"),
                "&8" + ChatColor.stripColor(shop.getDescription())
        ));
        menu.setItem(49, info);

        if (viewer.getUniqueId().equals(shop.getOwner())) {
            ItemStack manage = item(Material.ANVIL, "&aManage Shop", List.of("&7Open the shop editor."));
            ItemMeta meta = manage.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
                manage.setItemMeta(meta);
            }
            menu.setItem(48, manage);
        }

        ItemStack close = item(Material.BARRIER, "&cBack", List.of("&7Return to the browser."));
        menu.setItem(50, close);

        viewer.openInventory(menu);
    }

    public static boolean isShopView(String title) {
        return ChatColor.stripColor(title).startsWith(ChatColor.stripColor(TITLE_PREFIX));
    }

    public static NamespacedKey shopKey() {
        return SHOP_KEY;
    }

    public static NamespacedKey itemSlotKey() {
        return ITEM_SLOT;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Util.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(Util.color(line));
            }
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }
}
