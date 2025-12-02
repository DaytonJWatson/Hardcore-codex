package com.daytonjwatson.hardcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.ChatColor;

import com.daytonjwatson.hardcore.utils.MessageStyler;

public class RulesCommand implements CommandExecutor {
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        MessageStyler.sendPanel(sender, "Server Rules",
                ChatColor.GOLD + "Gameplay Rules",
                "PvP & Griefing allowed but " + ChatColor.RED + "strongly frowned upon" + ChatColor.GRAY + ".",
                "Stealing is allowed.",
                "No land claiming or protection.",
                ChatColor.GOLD + "Behavior Rules",
                "No hacking, cheating, or unfair clients.",
                "No exploiting glitches or dupes.",
                "No spam or harassment in chat.",
                "Respect other players—even if PvP is allowed.");

        return true;
    }
}
