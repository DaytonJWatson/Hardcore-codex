package com.daytonjwatson.hardcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;

public class GuideCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        sendGuideChat(player);
        giveGuideBook(player);

        return true;
    }

    private void sendGuideChat(Player p) {
        var lines = ConfigValues.guideChatLines();
        MessageStyler.sendPanel(p, ConfigValues.translateColor(ConfigValues.guideChatTitle()),
                lines.toArray(new String[0]));
    }

    private void giveGuideBook(Player player) {

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

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), book);
            MessageStyler.sendPanel(player, "Guide Delivered",
                    Util.color("&eYour inventory was full."),
                    Util.color("&7The Bandits & Heroes guide was dropped at your feet."));
        } else {
            player.getInventory().addItem(book);
            MessageStyler.sendPanel(player, "Guide Delivered",
                    Util.color("&6You received the &4Bandits & Heroes&6 guide book."));
        }
    }
}
