package com.daytonjwatson.hardcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        sender.sendMessage(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "========== HARDCORE HELP =========="
        );

        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "One-Life Server:");
        sender.sendMessage(ChatColor.GRAY + "If you die, you are "
                + ChatColor.DARK_RED + "" + ChatColor.BOLD + "PERMANENTLY BANNED" 
                + ChatColor.GRAY + ".");

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "General Info:");
        sender.sendMessage(ChatColor.GRAY + "• PvP & Griefing is allowed (but frowned upon).");
        sender.sendMessage(ChatColor.GRAY + "• Stealing is allowed.");
        sender.sendMessage(ChatColor.GRAY + "• No land claiming or protection.");
        sender.sendMessage(ChatColor.GRAY + "• No second chances.");

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Useful Commands:");
        sender.sendMessage(ChatColor.RED + "/guide "
                + ChatColor.GRAY + "- Show guide to Bandits and Heros");
        sender.sendMessage(ChatColor.RED + "/help "
                + ChatColor.GRAY + "- Show this help menu");
        sender.sendMessage(ChatColor.RED + "/rules "
                + ChatColor.GRAY + "- View hardcore server rules");
        sender.sendMessage(ChatColor.RED + "/stats "
                + ChatColor.GRAY + "- View your player stats");

        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Play cautiously.");
        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You only get one life.");

        return true;
    }
}
