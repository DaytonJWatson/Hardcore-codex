package com.daytonjwatson.hardcore.commands;

import java.util.Map;

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

        if (BanditTrackerManager.isOnCooldown(player)) {
            long remaining = BanditTrackerManager.getRemainingCooldownSeconds(player);
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.RED + "Tracker recalibrating...",
                    ChatColor.GRAY + "Try again in " + ChatColor.WHITE + remaining + "s" + ChatColor.GRAY + ".");
            return true;
        }

        ItemStack tracker = BanditTrackerManager.findTracker(player);
        boolean createdNew = false;

        if (tracker == null) {
            tracker = BanditTrackerManager.createTrackerItem();
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(tracker);
            if (!leftovers.isEmpty()) {
                for (ItemStack drop : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.YELLOW + "Your inventory was full.",
                        ChatColor.GRAY + "The tracker was dropped at your feet.");
            } else {
                createdNew = true;
            }
        }

        boolean foundTarget = BanditTrackerManager.updateTracker(player, tracker);
        BanditTrackerManager.markTrackerUsed(player);

        if (foundTarget) {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.GRAY + "Compass tuned to the closest bandit's trail.",
                    ChatColor.GRAY + "Follow the needle; it points to their last known heading.");
        } else {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.RED + "No bandits are currently online to track.");
        }

        if (createdNew) {
            MessageStyler.sendPanel(player, "Tracker Added",
                    ChatColor.GOLD + "You received a " + ChatColor.DARK_RED + "Bandit Tracker" + ChatColor.GOLD + ".",
                    ChatColor.GRAY + "Use it anytime to update the target.");
        }

        return true;
    }
}
