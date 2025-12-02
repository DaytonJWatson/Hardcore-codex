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
import com.daytonjwatson.hardcore.managers.BanditTrackerManager.TrackerResult;

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
        long remainingCooldown = BanditTrackerManager.getRemainingCooldownMillis(player);
        if (remainingCooldown > 0) {
            double secondsLeft = remainingCooldown / 1000.0;
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.RED + "Recalibrating...",
                    ChatColor.GRAY + "Try again in " + ChatColor.WHITE
                            + String.format("%.1f", secondsLeft) + ChatColor.GRAY + "s.");
            return;
        }

        BanditTrackerManager.markTrackerUsed(player);
        TrackerResult result = BanditTrackerManager.updateTracker(player, item);

        if (result.hasTarget()) {
            String summary = result.isSameWorld()
                    ? ChatColor.GRAY + "Signal locked ~" + ChatColor.WHITE + result.formatDistance()
                            + ChatColor.GRAY + "m away."
                    : ChatColor.GRAY + "Signal locked in " + ChatColor.WHITE + result.getWorldName() + ChatColor.GRAY + ".";

            MessageStyler.sendPanel(player, "Bandit Tracker", summary,
                    ChatColor.GRAY + "Follow the compass to close the gap.");
        } else {
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.RED + "No bandits online to track.");
        }
    }
}
