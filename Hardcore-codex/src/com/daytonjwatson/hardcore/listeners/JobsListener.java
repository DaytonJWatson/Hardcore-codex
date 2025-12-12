package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.views.JobsGui;

public class JobsListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        JobsManager.get().handleKill(killer, event.getEntity());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        JobsManager jobs = JobsManager.get();
        jobs.handleCropBreak(player, block);
        jobs.handleLogBreak(player, block.getType());
        jobs.handleOreBreak(player, block.getType());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlockPlaced().getType();
        JobsManager.get().handleBuild(player, type);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        JobsManager.get().handleFish(player, event);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        JobsManager.get().handleTravel(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(JobsGui.TITLE)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        JobsManager jobs = JobsManager.get();
        int slot = event.getRawSlot();
        int[] slots = {10, 12, 14, 16, 28, 30, 32};
        for (int i = 0; i < slots.length && i < com.daytonjwatson.hardcore.jobs.Occupation.values().length; i++) {
            if (slot == slots[i]) {
                com.daytonjwatson.hardcore.jobs.Occupation occupation = com.daytonjwatson.hardcore.jobs.Occupation.values()[i];
                jobs.setOccupation(player, occupation);
                player.closeInventory();
                return;
            }
        }

        if (slot == 49 && jobs.getOccupation(player.getUniqueId()) != null) {
            jobs.clearOccupation(player);
            player.closeInventory();
        }
    }
}
