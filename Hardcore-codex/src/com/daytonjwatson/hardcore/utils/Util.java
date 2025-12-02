package com.daytonjwatson.hardcore.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.daytonjwatson.hardcore.config.ConfigValues;

public class Util {
	
	public static String color(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
        public static void giveHardcoreGuideBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) return;

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
}