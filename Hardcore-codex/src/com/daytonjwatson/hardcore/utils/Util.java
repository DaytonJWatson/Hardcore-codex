package com.daytonjwatson.hardcore.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.shop.PlayerShop;

public class Util {
	
    public static String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static String plainName(ItemStack item) {
        if (item == null) {
            return "";
        }
        String display = null;
        if (item.hasItemMeta() && item.getItemMeta() != null) {
            display = item.getItemMeta().getDisplayName();
        }
        if (display != null && !display.isBlank()) {
            return ChatColor.stripColor(display);
        }
        return formatMaterialName(item.getType());
    }

    private static final NamespacedKey GUIDE_KEY = new NamespacedKey(HardcorePlugin.getInstance(),
            "guide_book_received");

    public static boolean giveHardcoreGuideBook(Player player) {
        if (player.getPersistentDataContainer().has(GUIDE_KEY, PersistentDataType.BYTE)) {
            return false;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null)
            return false;

        meta.setTitle(ConfigValues.translateColor(ConfigValues.guideBookTitle()));
        meta.setAuthor(ConfigValues.translateColor(ConfigValues.guideBookAuthor()));

        var pages = ConfigValues.guideBookPages();
        if (!pages.isEmpty()) {
            meta.addPage(pages.toArray(new String[0]));
        }

        book.setItemMeta(meta);

        var leftover = player.getInventory().addItem(book);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), book);
        }

        player.getPersistentDataContainer().set(GUIDE_KEY, PersistentDataType.BYTE, (byte) 1);
        return true;
    }

    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "permanently";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (days == 0 && hours == 0 && minutes == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
            if (c == ' ') {
                capitalizeNext = true;
            }
        }
        return builder.toString();
    }

    public static String resolveShopOwnerName(PlayerShop shop) {
        if (shop.getOwner().equals(ConfigValues.serverShopOwner())) {
            return ConfigValues.serverShopOwnerName();
        }

        OfflinePlayer owner = shop.getOwnerPlayer();
        String name = owner.getName();
        return name == null ? "Unknown" : name;
    }
}