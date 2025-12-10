package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.jobs.ActiveJob;
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
        JobsManager jobs = JobsManager.get();
        HardcorePlugin plugin = HardcorePlugin.getInstance();
        if (plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> jobs.handleCollection(player, stack.getType(), stack.getAmount()));
        } else {
            jobs.handleCollection(player, stack.getType(), stack.getAmount());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        JobsManager jobs = JobsManager.get();
        ActiveJob active = jobs.getActiveJob(player.getUniqueId());
        if (active != null) {
            for (com.daytonjwatson.hardcore.jobs.ActiveObjective objective : active.getPendingObjectives()) {
                if (objective.getDefinition().getType() == com.daytonjwatson.hardcore.jobs.JobType.MINE_BLOCK
                        && objective.getDefinition().shouldConsumeItems()
                        && objective.getDefinition().matches(type)) {
                    event.setDropItems(false);
                    break;
                }
            }
        }
        jobs.handleMine(player, type, 1);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlockPlaced().getType();
        JobsManager.get().handlePlace(player, type, 1);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block targetBlock = event.getBlockClicked().getRelative(event.getBlockFace());
        Material placed = targetBlock.getType();
        JobsManager.get().handlePlace(player, placed, 1);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !item.getType().name().endsWith("_HOE")) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        Material original = clicked.getType();
        if (original != Material.DIRT && original != Material.GRASS_BLOCK && original != Material.DIRT_PATH
                && original != Material.ROOTED_DIRT) {
            return;
        }

        HardcorePlugin plugin = HardcorePlugin.getInstance();
        Runnable check = () -> {
            if (clicked.getType() == Material.FARMLAND) {
                JobsManager.get().handlePlace(event.getPlayer(), Material.FARMLAND, 1);
            }
        };
        if (plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin, check);
        } else {
            check.run();
        }
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
        JobsManager jobs = JobsManager.get();
        HardcorePlugin plugin = HardcorePlugin.getInstance();
        if (plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> jobs.handleFish(player, stack.getType(), stack.getAmount()));
        } else {
            jobs.handleFish(player, stack.getType(), stack.getAmount());
        }
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        JobsManager.get().handleSmelt(player, event.getItemType(), event.getItemAmount());
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        int amountCrafted;
        if (event.isShiftClick()) {
            amountCrafted = calculateShiftCraftAmount(event.getInventory(), result);
        } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == result.getType()) {
            amountCrafted = event.getCurrentItem().getAmount();
        } else {
            amountCrafted = result.getAmount();
        }

        JobsManager jobs = JobsManager.get();
        HardcorePlugin plugin = HardcorePlugin.getInstance();
        if (plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> jobs.handleCraft(player, result.getType(), amountCrafted));
        } else {
            jobs.handleCraft(player, result.getType(), amountCrafted);
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        ItemStack item = event.getItem();
        JobsManager jobs = JobsManager.get();
        HardcorePlugin plugin = HardcorePlugin.getInstance();
        if (plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> jobs.handleEnchant(player, item.getType()));
        } else {
            jobs.handleEnchant(player, item.getType());
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }
        JobsManager.get().handleBreed(player, event.getEntity().getType());
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        JobsManager.get().handleTame(player, event.getEntity().getType());
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

    private int calculateShiftCraftAmount(CraftingInventory inventory, ItemStack result) {
        ItemStack[] matrix = inventory.getMatrix();
        int craftsPossible = Integer.MAX_VALUE;
        for (ItemStack stack : matrix) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            craftsPossible = Math.min(craftsPossible, stack.getAmount());
        }
        if (craftsPossible == Integer.MAX_VALUE) {
            return result.getAmount();
        }
        return result.getAmount() * craftsPossible;
    }
}
