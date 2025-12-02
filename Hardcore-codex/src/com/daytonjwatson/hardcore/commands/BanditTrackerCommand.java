package com.daytonjwatson.hardcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.managers.BanditTrackerManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;

public class BanditTrackerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("recipe")) {
            BanditTrackerManager.openRecipePreview(player);
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.GRAY + "This is the custom crafting recipe.",
                    ChatColor.GRAY + "Use your tracker with " + ChatColor.WHITE + "sneak + right click"
                            + ChatColor.GRAY + " to lock onto bandits.");
            return true;
        }

        if (BanditTrackerManager.isOnCooldown(player)) {
            long remaining = BanditTrackerManager.getRemainingCooldownSeconds(player);
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.RED + "Tracker recalibrating...",
                    ChatColor.GRAY + "Try again in " + ChatColor.WHITE + remaining + "s" + ChatColor.GRAY + ".",
                    ChatColor.DARK_GRAY + "Tip: /bandittracker recipe to view the pattern.");
            return true;
        }

        ItemStack tracker = BanditTrackerManager.findTracker(player);

        if (tracker == null) {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.RED + "You don't have a tracker yet.",
                    ChatColor.GRAY + "Craft one with the custom recipe (Compass core surrounded by redstone, gold, ender pearls, and iron).",
                    ChatColor.GRAY + "Once crafted, sneak-right-click to calibrate it.",
                    ChatColor.DARK_GRAY + "Use /bandittracker recipe for a visual guide.");
            return true;
        }

        boolean foundTarget = BanditTrackerManager.updateTracker(player, tracker);
        BanditTrackerManager.markTrackerUsed(player);

        if (foundTarget) {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.GRAY + "Compass tuned to the closest bandit's trail.",
                    ChatColor.GRAY + "Sneak while using it in-hand to keep the signal stable.",
                    ChatColor.DARK_GRAY + "Need the recipe? /bandittracker recipe");
        } else {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.RED + "No bandits are currently online to track.",
                    ChatColor.DARK_GRAY + "Reminder: /bandittracker recipe shows how to craft one.");
        }

        return true;
    }
}
