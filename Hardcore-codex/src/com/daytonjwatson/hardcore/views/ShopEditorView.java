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
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.utils.Util;

public final class ShopEditorView {

    public static final String TITLE = Util.color("&6&lShop Editor");
    private static final NamespacedKey SHOP_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_editor_id");
    private static final NamespacedKey ACTION_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_editor_action");

    private ShopEditorView() {}

    public static void open(Player player, PlayerShop shop) {
        Inventory menu = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        menu.setItem(11, action(Material.NAME_TAG, "&eRename Shop",
                List.of("&7Current: &f" + shop.getName(), "&8Click to enter a new name."), "rename", shop));
        menu.setItem(12, action(Material.WRITABLE_BOOK, "&eEdit Description",
                List.of("&7Current:", "&f" + shop.getDescription(), "&8Click to type a new description."), "description",
                shop));
        menu.setItem(13, action(Material.ITEM_FRAME, "&eSet Display Item",
                List.of("&7Use the item in your hand.", "&8Click to update the icon."), "icon", shop));
        menu.setItem(14, action(shop.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
                shop.isOpen() ? "&aOpen Shop" : "&cClosed Shop",
                List.of("&7Toggle whether players can buy."), "toggle", shop));
        menu.setItem(15, action(Material.CHEST, "&aManage Stock",
                List.of("&7Add or remove shop listings."), "stock", shop));
        menu.setItem(21, action(shop.isNotificationsEnabled() ? Material.BELL : Material.NOTE_BLOCK,
                shop.isNotificationsEnabled() ? "&aNotifications Enabled" : "&cNotifications Disabled",
                List.of("&7Alerts you when items sell", "&7or sell out."), "notifications", shop));
        menu.setItem(20, action(Material.REDSTONE_BLOCK, "&cDelete Shop",
                List.of("&7Remove this shop and return", "&7all listed items to you."), "delete", shop));

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to your shops."));
        menu.setItem(22, back);

        player.openInventory(menu);
    }

    public static boolean isEditor(String title) {
        return Util.color(TITLE).equals(title) || Util.color(title).equals(TITLE);
    }

    public static NamespacedKey shopKey() {
        return SHOP_KEY;
    }

    public static NamespacedKey actionKey() {
        return ACTION_KEY;
    }

    private static ItemStack action(Material material, String name, List<String> lore, String action, PlayerShop shop) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
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
