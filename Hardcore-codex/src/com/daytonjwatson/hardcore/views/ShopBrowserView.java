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
import com.daytonjwatson.hardcore.managers.ShopManager;
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.utils.Util;

public final class ShopBrowserView {

    public static final String TITLE = Util.color("&6&lPlayer Shops");
    private static final NamespacedKey SHOP_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_browser_target");
    private static final NamespacedKey NAV_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "shop_browser_nav");
    private static final int PAGE_SIZE = 45;

    private ShopBrowserView() {}

    public static void open(Player player, int page) {
        ShopManager manager = ShopManager.get();
        List<PlayerShop> shops = manager.getSortedShops();
        int totalPages = Math.max(1, (int) Math.ceil(shops.size() / (double) PAGE_SIZE));
        int current = Math.max(0, Math.min(page, totalPages - 1));

        Inventory menu = Bukkit.createInventory(null, 54, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        int slot = 0;
        int start = current * PAGE_SIZE;
        for (int i = start; i < Math.min(shops.size(), start + PAGE_SIZE); i++) {
            PlayerShop shop = shops.get(i);
            ItemStack icon = shop.getIcon();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Util.color("&7Owner: &f" + Util.resolveShopOwnerName(shop)));
                lore.add(Util.color("&7Status: " + (shop.isOpen() ? "&aOpen" : "&cClosed")));
                lore.add(Util.color("&7Listings: &f" + shop.getStock().size() + "&7/27"));
                lore.add(Util.color("&8Click to view this shop."));
                meta.setDisplayName(Util.color(shop.getName()));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, shop.getId().toString());
                icon.setItemMeta(meta);
            }
            menu.setItem(slot++, icon);
        }

        addNavigation(menu, current, totalPages);
        player.openInventory(menu);
    }

    private static void addNavigation(Inventory menu, int currentPage, int totalPages) {
        ItemStack close = item(Material.BARRIER, "&cClose", List.of("&7Close the shop browser."));
        menu.setItem(49, close);

        ItemStack indicator = item(Material.NAME_TAG, "&ePage " + (currentPage + 1) + " of " + totalPages,
                List.of("&7Browse every shop on the server."));
        menu.setItem(53, indicator);

        if (currentPage > 0) {
            menu.setItem(45, navigationItem("&aPrevious", List.of("&7Go to the previous page."), currentPage - 1));
        }
        if (currentPage < totalPages - 1) {
            menu.setItem(46, navigationItem("&aNext", List.of("&7Go to the next page."), currentPage + 1));
        }
    }

    public static boolean isBrowser(String title) {
        return ChatColor.stripColor(title).equals(ChatColor.stripColor(TITLE));
    }

    public static NamespacedKey shopKey() {
        return SHOP_KEY;
    }

    public static NamespacedKey navKey() {
        return NAV_KEY;
    }

    public static ItemStack navigationItem(String name, List<String> lore, int page) {
        ItemStack item = item(Material.ARROW, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(NAV_KEY, PersistentDataType.INTEGER, page);
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
