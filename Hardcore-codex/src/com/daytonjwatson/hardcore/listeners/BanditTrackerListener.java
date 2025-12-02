package com.daytonjwatson.hardcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.managers.BanditTrackerManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;

public class BanditTrackerListener implements Listener {

    @EventHandler
    public void onTrackerUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only react to main-hand interactions
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!BanditTrackerManager.isTracker(item)) {
            return;
        }

        Player player = event.getPlayer();
        boolean found = BanditTrackerManager.updateTracker(player, item);

        if (found) {
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.GRAY + "Target refreshed.",
                    ChatColor.GRAY + "Follow the compass to reach the bandit.");
        } else {
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.RED + "No bandits online to track.");
        }
    }
}
