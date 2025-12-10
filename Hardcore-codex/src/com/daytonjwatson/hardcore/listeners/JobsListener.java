package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.jobs.JobDefinition;
import com.daytonjwatson.hardcore.jobs.JobsManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.JobsGui;

public class JobsListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        JobsManager.get().handleKill(killer, event.getEntityType());
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        JobsManager.get().handleCollection(player, stack.getType(), stack.getAmount());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        JobsManager.get().handleCollection(player, type, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(JobsGui.TITLE) && !title.equals(JobsGui.ACTIVE_TITLE)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        JobsManager jobs = JobsManager.get();

        if (title.equals(JobsGui.TITLE)) {
            int slot = event.getRawSlot();
            int[] slots = {11, 13, 15};
            for (int i = 0; i < slots.length; i++) {
                if (slot == slots[i]) {
                    if (jobs.getActiveJob(player.getUniqueId()) != null) {
                        player.sendMessage(Util.color("&cYou already have an active job."));
                        return;
                    }

                    java.util.List<JobDefinition> offers = jobs.getOfferedJobs(player.getUniqueId());
                    if (offers.size() <= i) {
                        player.sendMessage(Util.color("&cThat offer is no longer available."));
                        return;
                    }
                    jobs.assignJob(player, offers.get(i));
                    player.closeInventory();
                    return;
                }
            }

            if (slot == 26 && jobs.getActiveJob(player.getUniqueId()) != null) {
                JobsGui.openActive(player);
            }
        } else if (title.equals(JobsGui.ACTIVE_TITLE)) {
            int slot = event.getRawSlot();
            if (slot == 22) {
                jobs.abandonJob(player);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        JobsManager.get().clearOffers(event.getPlayer().getUniqueId());
    }
}
