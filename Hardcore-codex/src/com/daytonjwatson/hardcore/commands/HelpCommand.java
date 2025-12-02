package com.daytonjwatson.hardcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.ChatColor;

import com.daytonjwatson.hardcore.utils.MessageStyler;

public class HelpCommand implements CommandExecutor {
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        MessageStyler.sendPanel(sender, "Hardcore Help",
                "One-Life Server: " + ChatColor.RED + "Death = Permanent Ban",
                "PvP & Griefing allowed (but frowned upon).",
                "Stealing allowed. No land claiming or protection.",
                "Useful Commands:",
                ChatColor.RED + "/guide " + ChatColor.GRAY + "- Bandits & Heroes guide",
                ChatColor.RED + "/help " + ChatColor.GRAY + "- Show this help menu",
                ChatColor.RED + "/rules " + ChatColor.GRAY + "- Hardcore server rules",
                ChatColor.RED + "/stats " + ChatColor.GRAY + "- View player stats",
                ChatColor.RED + "/bandittracker " + ChatColor.GRAY + "- Track the nearest bandit",
                ChatColor.DARK_RED + "Play cautiously. You only get one life.");

        return true;
    }
}
