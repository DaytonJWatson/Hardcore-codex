package com.daytonjwatson.hardcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        boolean hadSpace = player.getInventory().firstEmpty() != -1;

        if (!Util.giveHardcoreGuideBook(player)) {
            MessageStyler.sendPanel(player, "Guide Delivered",
                    Util.color("&7You already received the Hardcore Guide."),
                    Util.color("&7Hang onto your first copy—it's one per player."));
            return;
        }

        if (hadSpace) {
            MessageStyler.sendPanel(player, "Guide Delivered",
                    Util.color("&6You received the &cHardcore Guide&6."));
        } else {
            MessageStyler.sendPanel(player, "Guide Delivered",
                    Util.color("&eYour inventory was full."),
                    Util.color("&7The Hardcore Guide was dropped at your feet."));
        }
    }
}
