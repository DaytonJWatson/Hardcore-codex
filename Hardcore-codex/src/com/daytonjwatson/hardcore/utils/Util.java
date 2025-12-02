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

        meta.setTitle("§4§lHardcore Survival Guide");
        meta.setAuthor("HardcoreGuide");

        meta.addPage(
                "§4§lHARDCORE MODE\n§0\n" +
                "§c§lONE LIFE ONLY.\n" +
                "§0If you die, you are\n§4§lPERMANENTLY BANNED.\n§0\n" +
                "Read carefully. Survive smart."
        );

        meta.addPage(
                "§6§lGENERAL RULES\n\n" +
                "§c• Treat every heart as final.\n" +
                "• Never log off in danger.\n" +
                "• Avoid risky caves early.\n" +
                "• §lRetreat early, not late.§r"
        );

        meta.addPage(
                "§2§lEARLY GAME PRIORITIES\n\n" +
                "§a1) Craft a §lshield§r§a immediately.\n" +
                "2) Get full §lIron Armor.\n" +
                "3) Secure a food source.\n" +
                "4) Build a safe lit base.\n"
        );

        meta.addPage(
                "§9§lCOMBAT TIPS\n\n" +
                "§b• Always use a shield.\n" +
                "• Avoid fighting at night.\n" +
                "• Bow > melee for safety.\n" +
                "• Don’t fight with lag.\n"
        );

        meta.addPage(
                "§5§lNETHER SAFETY\n\n" +
                "§d• Bring extra blocks, food.\n" +
                "• Cauldron + water for fire.\n" +
                "• Build a safe portal room.\n" +
                "• Watch for fall damage.\n"
        );

        meta.addPage(
                "§3§lFALL & VOID SAFETY\n\n" +
                "§b• Never bridge without sneaking.\n" +
                "• Use rails or guard blocks.\n" +
                "• Feather Falling helps.\n" +
                "• Water bucket = lifesaver.\n"
        );

        meta.addPage(
                "§e§lHARDCORE MINDSET\n\n" +
                "§6• If it feels dangerous,\n  don't do it.\n" +
                "• Overprepare always.\n" +
                "• Take no unnecessary risks.\n" +
                "• One mistake = game over.\n"
        );

        meta.addPage(
                "§4§lSERVER RULE\n\n" +
                "§cThis world is Hardcore-only.\n" +
                "§cOne life. No exceptions.\n\n" +
                "§4If you die:\n§4§lPERM BAN FOREVER.\n\n" +
                "§0Good luck, survivor."
        );

        book.setItemMeta(meta);

        var leftover = player.getInventory().addItem(book);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), book);
        }
    }
}