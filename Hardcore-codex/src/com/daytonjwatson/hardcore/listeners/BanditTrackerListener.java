package com.daytonjwatson.hardcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

        if (!player.isSneaking()) {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.YELLOW + "Steady your hands.",
                    ChatColor.GRAY + "Sneak while using the tracker to calibrate it.");
            return;
        }

        if (player.getVelocity().lengthSquared() > 0.05) {
            MessageStyler.sendPanel(player, "Bandit Tracker",
                    ChatColor.YELLOW + "Hold still to get a clean reading.");
            return;
        }

        if (BanditTrackerManager.isOnCooldown(player)) {
            long remaining = BanditTrackerManager.getRemainingCooldownSeconds(player);
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.RED + "Tracker cooling down.",
                    ChatColor.GRAY + "Try again in " + ChatColor.WHITE + remaining + "s" + ChatColor.GRAY + ".");
            return;
        }

        boolean found = BanditTrackerManager.updateTracker(player, item);
        BanditTrackerManager.markTrackerUsed(player);

        if (found) {
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.GRAY + "Compass locked onto a nearby bandit.",
                    ChatColor.GRAY + "Needle points to their last known location; stay alert.");
        } else {
            MessageStyler.sendPanel(player, "Bandit Tracker", ChatColor.RED + "No bandits online to track.");
        }
    }

    @EventHandler
    public void onRecipeClick(InventoryClickEvent event) {
        if (!BanditTrackerManager.isRecipeInventory(event.getInventory())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onRecipeDrag(InventoryDragEvent event) {
        if (!BanditTrackerManager.isRecipeInventory(event.getInventory())) {
            return;
        }

        event.setCancelled(true);
    }
}
