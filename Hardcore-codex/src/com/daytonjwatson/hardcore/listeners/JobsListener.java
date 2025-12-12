package com.daytonjwatson.hardcore.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.jobs.Occupation;
import com.daytonjwatson.hardcore.views.JobsGui;

public class JobsListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        JobsManager.get().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        JobsManager.get().handleQuit(event.getPlayer());
    }

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
        jobs.handleLogBreak(player, block);
        jobs.handleOreBreak(player, block);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        JobsManager jobs = JobsManager.get();
        jobs.trackPlacement(player.getUniqueId(), event.getBlockPlaced());
        jobs.handleBuild(player, event.getBlockPlaced());
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
        JobsManager jobs = JobsManager.get();
        jobs.markMovement(event.getPlayer().getUniqueId());
        jobs.handleTravel(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!JobsGui.isJobsMenu(title)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        JobsManager jobs = JobsManager.get();
        if (JobsGui.isConfirm(title)) {
            int slot = event.getRawSlot();
            if (slot == JobsGui.CONFIRM_SLOT) {
                Occupation pending = jobs.popPendingSelection(player.getUniqueId());
                if (pending != null) {
                    jobs.setOccupation(player, pending, false);
                }
                player.closeInventory();
            } else if (slot == JobsGui.CANCEL_SLOT) {
                jobs.popPendingSelection(player.getUniqueId());
                JobsGui.open(player);
            }
            return;
        }

        int slot = event.getRawSlot();
        Occupation[] values = Occupation.values();
        int[] slots = JobsGui.getOccupationSlots();
        for (int i = 0; i < slots.length && i < values.length; i++) {
            if (slot == slots[i]) {
                if (jobs.getOccupation(player.getUniqueId()) != null) {
                    player.sendMessage(com.daytonjwatson.hardcore.utils.Util
                            .color("&cYou already have a permanent occupation."));
                    player.closeInventory();
                    return;
                }
                jobs.queueSelection(player, values[i]);
                JobsGui.openConfirm(player, values[i]);
                return;
            }
        }
    }
}
