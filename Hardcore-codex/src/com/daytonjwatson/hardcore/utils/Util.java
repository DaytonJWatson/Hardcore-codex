package com.daytonjwatson.hardcore.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class Util {
	
	public static String color(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
	public static void giveHardcoreGuideBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) return;

        meta.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Hardcore Survival Guide");
        meta.setAuthor("HardcoreGuide");

        meta.addPage(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "HARDCORE MODE\n\n" +
                        ChatColor.GRAY + "One life only.\n" +
                        ChatColor.GRAY + "Death = " + ChatColor.DARK_RED + "PERMANENT BAN\n\n" +
                        ChatColor.DARK_GRAY + "Read carefully. Survive smart."
        );

        meta.addPage(
                ChatColor.GOLD + "" + ChatColor.BOLD + "GENERAL RULES\n\n" +
                        ChatColor.GRAY + "⟡ Treat every heart as final.\n" +
                        ChatColor.GRAY + "⟡ Never log off in danger.\n" +
                        ChatColor.GRAY + "⟡ Avoid risky caves early.\n" +
                        ChatColor.GRAY + "⟡ Retreat early, not late."
        );

        meta.addPage(
                ChatColor.GOLD + "" + ChatColor.BOLD + "EARLY GAME PRIORITIES\n\n" +
                        ChatColor.GRAY + "⟡ Craft a shield immediately.\n" +
                        ChatColor.GRAY + "⟡ Get full Iron Armor.\n" +
                        ChatColor.GRAY + "⟡ Secure a food source.\n" +
                        ChatColor.GRAY + "⟡ Build a safe lit base."
        );

        meta.addPage(
                ChatColor.GOLD + "" + ChatColor.BOLD + "COMBAT TIPS\n\n" +
                        ChatColor.GRAY + "⟡ Always use a shield.\n" +
                        ChatColor.GRAY + "⟡ Avoid fighting at night.\n" +
                        ChatColor.GRAY + "⟡ Bow > melee for safety.\n" +
                        ChatColor.GRAY + "⟡ Don’t fight with lag."
        );

        meta.addPage(
                ChatColor.GOLD + "" + ChatColor.BOLD + "NETHER SAFETY\n\n" +
                        ChatColor.GRAY + "⟡ Bring extra blocks & food.\n" +
                        ChatColor.GRAY + "⟡ Cauldron + water for fire.\n" +
                        ChatColor.GRAY + "⟡ Build a safe portal room.\n" +
                        ChatColor.GRAY + "⟡ Watch for fall damage."
        );

        meta.addPage(
                ChatColor.GOLD + "" + ChatColor.BOLD + "FALL & VOID SAFETY\n\n" +
                        ChatColor.GRAY + "⟡ Never bridge without sneaking.\n" +
                        ChatColor.GRAY + "⟡ Use rails or guard blocks.\n" +
                        ChatColor.GRAY + "⟡ Feather Falling helps.\n" +
                        ChatColor.GRAY + "⟡ Water bucket = lifesaver."
        );

        meta.addPage(
                ChatColor.GOLD + "" + ChatColor.BOLD + "HARDCORE MINDSET\n\n" +
                        ChatColor.GRAY + "⟡ If it feels dangerous, don’t do it.\n" +
                        ChatColor.GRAY + "⟡ Overprepare always.\n" +
                        ChatColor.GRAY + "⟡ Take no unnecessary risks.\n" +
                        ChatColor.GRAY + "⟡ One mistake = game over."
        );

        meta.addPage(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "SERVER RULE\n\n" +
                        ChatColor.RED + "This world is Hardcore-only.\n" +
                        ChatColor.RED + "One life. No exceptions.\n\n" +
                        ChatColor.DARK_RED + "If you die: PERM BAN FOREVER.\n\n" +
                        ChatColor.GRAY + "Good luck, survivor."
        );

        book.setItemMeta(meta);

        var leftover = player.getInventory().addItem(book);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), book);
        }
    }
}