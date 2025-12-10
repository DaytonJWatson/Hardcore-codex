package com.daytonjwatson.hardcore.views;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
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

public final class ShopStockView {

    public static final String TITLE = Util.color("&6&lShop Stock");
    private static final NamespacedKey SHOP_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_stock_id");
    private static final NamespacedKey SLOT_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_stock_slot");

    private ShopStockView() {}

    public static void open(Player player, PlayerShop shop) {
        Inventory menu = Bukkit.createInventory(null, 54, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        BankManager bank = BankManager.get();
        int slot = 0;
        for (int i = 0; i < 27; i++) {
            ShopItem entry = shop.getStock().get(i);
            if (entry == null) continue;
            ItemStack display = entry.getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Util.color("&7Price: &f" + bank.formatCurrency(entry.getPrice())));
                lore.add(Util.color("&8Left-click to remove."));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
                meta.getPersistentDataContainer().set(SLOT_KEY, PersistentDataType.INTEGER, i);
                display.setItemMeta(meta);
            }
            menu.setItem(slot++, display);
        }

        ItemStack add = item(Material.EMERALD_BLOCK, "&aAdd Item From Hand", List.of(
                "&7Place the item you want to sell in your hand.",
                "&8Click here to price and add it."
        ));
        ItemMeta addMeta = add.getItemMeta();
        if (addMeta != null) {
            addMeta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
            add.setItemMeta(addMeta);
        }
        menu.setItem(48, add);

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to shop editor."));
        menu.setItem(50, back);

        player.openInventory(menu);
    }

    public static boolean isStock(String title) {
        return Util.color(TITLE).equals(title) || Util.color(title).equals(TITLE);
    }

    public static NamespacedKey shopKey() {
        return SHOP_KEY;
    }

    public static NamespacedKey slotKey() {
        return SLOT_KEY;
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
