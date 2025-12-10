package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.jobs.JobOffer;
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
        JobsManager.get().handleMine(player, type, 1);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!(event.getState() == PlayerFishEvent.State.CAUGHT_FISH)) {
            return;
        }
        if (!(event.getCaught() instanceof org.bukkit.entity.Item item)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack stack = item.getItemStack();
        JobsManager.get().handleFish(player, stack.getType(), stack.getAmount());
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        JobsManager.get().handleCraft(player, result.getType(), result.getAmount());
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

                    java.util.List<JobOffer> offers = jobs.getOfferedJobs(player.getUniqueId());
                    if (offers.size() <= i) {
                        player.sendMessage(Util.color("&cThat offer is no longer available."));
                        return;
                    }
                    if (jobs.isSlotCoolingDown(player.getUniqueId(), i)) {
                        player.sendMessage(Util.color("&cThat option is on cooldown."));
                        return;
                    }
                    JobOffer offer = offers.get(i);
                    if (offer == null) {
                        player.sendMessage(Util.color("&cThat offer is currently unavailable."));
                        return;
                    }
                    jobs.assignJob(player, offer, i);
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
}
