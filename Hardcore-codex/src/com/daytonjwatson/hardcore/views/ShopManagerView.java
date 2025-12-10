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
import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.managers.ShopManager;
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.utils.Util;

public final class ShopManagerView {

    public static final String TITLE = Util.color("&6&lYour Shops");
    private static final NamespacedKey SHOP_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_manage_target");

    private ShopManagerView() {}

    public static void open(Player player) {
        ShopManager manager = ShopManager.get();
        List<PlayerShop> owned = manager.getShopsOwnedBy(player.getUniqueId());
        Inventory menu = Bukkit.createInventory(null, 27, TITLE);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        int slot = 10;
        for (PlayerShop shop : owned) {
            ItemStack icon = shop.getIcon();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Util.color("&7Status: " + (shop.isOpen() ? "&aOpen" : "&cClosed")));
                lore.add(Util.color("&7Listings: &f" + shop.getStock().size() + "&7/27"));
                lore.add(Util.color("&8Left-click to manage."));
                lore.add(Util.color("&8Right-click to delete."));
                meta.setLore(lore);
                meta.setDisplayName(Util.color(shop.getName()));
                meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
                icon.setItemMeta(meta);
            }
            menu.setItem(slot++, icon);
        }

        ItemStack create = item(Material.EMERALD, "&aCreate New Shop", List.of(
                "&7Cost: &f" + BankManager.get().formatCurrency(ConfigValues.shopCreationCost()),
                "&7Limit: &f" + ConfigValues.maxShopsPerPlayer() + " shops",
                "&8Click to spend and open a new shop."
        ));
        menu.setItem(16, create);

        ItemStack back = item(Material.BARRIER, "&cBack", List.of("&7Return to the browser."));
        menu.setItem(22, back);

        player.openInventory(menu);
    }

    public static boolean isManager(String title) {
        return Util.color(TITLE).equals(title) || Util.color(title).equals(TITLE);
    }

    public static NamespacedKey shopKey() {
        return SHOP_KEY;
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
