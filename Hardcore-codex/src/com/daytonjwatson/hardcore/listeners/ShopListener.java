package com.daytonjwatson.hardcore.listeners;

import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.ShopManager;
import com.daytonjwatson.hardcore.managers.ShopManager.ManageReopen;
import com.daytonjwatson.hardcore.managers.ShopManager.ManageView;
import com.daytonjwatson.hardcore.shop.PlayerShop;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.ShopBrowserView;
import com.daytonjwatson.hardcore.views.ShopEditorView;
import com.daytonjwatson.hardcore.views.ShopManagerView;
import com.daytonjwatson.hardcore.views.ShopStockView;
import com.daytonjwatson.hardcore.views.ShopView;

public class ShopListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (ShopBrowserView.isBrowser(title)) {
            handleBrowser(event, player);
            return;
        }
        if (ShopView.isShopView(title)) {
            handleShopView(event, player);
            return;
        }
        if (ShopManagerView.isManager(title)) {
            handleManager(event, player);
            return;
        }
        if (ShopEditorView.isEditor(title)) {
            handleEditor(event, player);
            return;
        }
        if (ShopStockView.isStock(title)) {
            handleStock(event, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (ShopStockView.isStock(title)) {
            ShopManager.get().clearActiveStockShop(player.getUniqueId());
        }
        if (ShopEditorView.isEditor(title)) {
            ShopManager.get().consumePendingIcon(player.getUniqueId());
        }
        if (!ShopView.isShopView(title)) {
            return;
        }

        UUID viewerId = player.getUniqueId();
        ShopManager manager = ShopManager.get();
        ShopManager.ViewSession session = manager.getViewSession(viewerId);
        if (session == null) {
            return;
        }

        UUID shopId = session.shopId();

        if (manager.consumeReopeningShop(viewerId, shopId)) {
            return;
        }

        ShopManager.ViewSession closingSession = manager.markSessionClosing(viewerId, shopId);
        if (closingSession == null) {
            return;
        }
        int sessionId = closingSession.sessionId();

        HardcorePlugin plugin = HardcorePlugin.getInstance();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (manager.isSessionActive(viewerId, shopId, sessionId)) {
                return;
            }
            manager.dispatchPurchaseSummary(player, shopId);
            manager.clearSession(viewerId, shopId, sessionId);
        }, 2L);
    }

    private void handleBrowser(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        Integer targetPage = container.get(ShopBrowserView.navKey(), PersistentDataType.INTEGER);
        if (targetPage != null) {
            ShopBrowserView.open(player, targetPage);
            return;
        }

        String shopId = container.get(ShopBrowserView.shopKey(), PersistentDataType.STRING);
        if (shopId != null) {
            try {
                PlayerShop shop = ShopManager.get().getShop(UUID.fromString(shopId));
                if (shop != null) {
                    ShopView.open(player, shop);
                }
            } catch (IllegalArgumentException ignored) {}
            return;
        }

        String name = meta.getDisplayName();
        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Manage Your Shops")) {
            ShopManagerView.open(player);
            return;
        }
        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Close")) {
            player.closeInventory();
        }
    }

    private void handleShopView(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String shopId = container.get(ShopView.shopKey(), PersistentDataType.STRING);
        if (shopId == null) {
            String name = meta.getDisplayName();
            if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Back")) {
                ShopBrowserView.open(player, 0);
            }
            return;
        }

        PlayerShop shop;
        try {
            shop = ShopManager.get().getShop(UUID.fromString(shopId));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (shop == null) return;

        Integer slot = container.get(ShopView.itemSlotKey(), PersistentDataType.INTEGER);
        if (slot == null) {
            if (player.getUniqueId().equals(shop.getOwner())) {
                ShopEditorView.open(player, shop);
            }
            return;
        }

        if (!shop.isOpen() && !player.getUniqueId().equals(shop.getOwner())) {
            player.sendMessage(Util.color("&cThis shop is closed."));
            return;
        }

        ClickType click = event.getClick();
        boolean buyStack = click.isRightClick();
        if (!click.isLeftClick() && !buyStack) {
            return;
        }

        ShopManager manager = ShopManager.get();
        boolean success = manager.processPurchase(player, shop.getId(), slot, buyStack);
        if (success) {
            manager.markReopeningShop(player.getUniqueId(), shop.getId());
            ShopView.open(player, shop);
        }
    }

    private void handleManager(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();
        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Back")) {
            ShopBrowserView.open(player, 0);
            return;
        }

        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Create New Shop")) {
            PlayerShop created = ShopManager.get().createShop(player);
            if (created != null) {
                ShopEditorView.open(player, created);
                return;
            }
            ShopManagerView.open(player);
            return;
        }

        String id = meta.getPersistentDataContainer().get(ShopManagerView.shopKey(), PersistentDataType.STRING);
        if (id != null) {
            try {
                PlayerShop shop = ShopManager.get().getShop(UUID.fromString(id));
                if (shop != null) {
                    ShopEditorView.open(player, shop);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void handleEditor(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ShopManager manager = ShopManager.get();
        if (clickedInventory != null && clickedInventory.equals(player.getInventory())
                && manager.isAwaitingIcon(player.getUniqueId())) {
            handleIconSelectionFromInventory(player, clicked);
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String name = meta.getDisplayName();
        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Back")) {
            ShopManagerView.open(player);
            return;
        }
        String idRaw = meta.getPersistentDataContainer().get(ShopEditorView.shopKey(), PersistentDataType.STRING);
        String action = meta.getPersistentDataContainer().get(ShopEditorView.actionKey(), PersistentDataType.STRING);
        if (idRaw == null || action == null) return;

        PlayerShop shop;
        try {
            shop = ShopManager.get().getShop(UUID.fromString(idRaw));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (shop == null) return;

        switch (action) {
            case "rename" -> {
                manager.setPendingRename(player.getUniqueId(), shop.getId());
                manager.setPendingManageReopen(player.getUniqueId(), ManageView.EDITOR, shop.getId());
                player.closeInventory();
                player.sendMessage(Util.color("&eType the new shop name in chat. Type &ccancel &eto abort."));
            }
            case "description" -> {
                manager.setPendingDescription(player.getUniqueId(), shop.getId());
                manager.setPendingManageReopen(player.getUniqueId(), ManageView.EDITOR, shop.getId());
                player.closeInventory();
                player.sendMessage(Util.color("&eType the new description in chat. Type &ccancel &eto abort."));
            }
            case "icon" -> {
                manager.setPendingIcon(player.getUniqueId(), shop.getId());
                player.sendMessage(Util.color(
                        "&eClick an item in your inventory to use it as this shop's display icon."));
            }
            case "toggle" -> {
                shop.setOpen(!shop.isOpen());
                ShopManager.get().save();
                ShopEditorView.open(player, shop);
                player.sendMessage(Util.color(shop.isOpen() ? "&aShop opened." : "&cShop closed."));
            }
            case "stock" -> ShopStockView.open(player, shop);
            case "notifications" -> {
                shop.setNotificationsEnabled(!shop.isNotificationsEnabled());
                ShopManager.get().save();
                ShopEditorView.open(player, shop);
                player.sendMessage(Util.color(shop.isNotificationsEnabled()
                        ? "&aShop notifications enabled."
                        : "&cShop notifications disabled."));
            }
            case "delete" -> {
                shop.getStock().values().forEach(item -> {
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.getItem().clone());
                    overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
                });
                ShopManager.get().deleteShop(shop.getId());
                ShopManagerView.open(player);
                player.sendMessage(Util.color("&cDeleted shop and returned any listed items."));
            }
            default -> {}
        }
    }

    private void handleIconSelectionFromInventory(Player player, ItemStack clicked) {
        ShopManager manager = ShopManager.get();
        UUID shopId = manager.consumePendingIcon(player.getUniqueId());
        if (shopId == null) {
            player.sendMessage(Util.color("&cNo shop selected for icon update."));
            return;
        }
        PlayerShop shop = manager.getShop(shopId);
        if (shop == null) {
            player.sendMessage(Util.color("&cThat shop no longer exists."));
            return;
        }

        ItemStack icon = clicked.clone();
        shop.setIcon(icon);
        manager.save();
        ShopEditorView.open(player, shop);
        player.sendMessage(Util.color("&aUpdated shop icon."));
    }

    private void handleStock(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clicked = event.getCurrentItem();
        if (clickedInventory == null || clicked == null || clicked.getType() == Material.AIR) return;

        if (clickedInventory.equals(player.getInventory())) {
            handleStockSelectionFromInventory(player, clicked, event.getSlot());
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String name = meta.getDisplayName();
        if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Back")) {
            ShopManagerView.open(player);
            return;
        }

        String idRaw = meta.getPersistentDataContainer().get(ShopStockView.shopKey(), PersistentDataType.STRING);
        if (idRaw == null) return;
        PlayerShop shop;
        try {
            shop = ShopManager.get().getShop(UUID.fromString(idRaw));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (shop == null) return;

        Integer slot = meta.getPersistentDataContainer().get(ShopStockView.slotKey(), PersistentDataType.INTEGER);
        if (slot != null) {
            ShopManager.get().getShop(shop.getId()).getStock().remove(slot);
            ShopManager.get().save();
            player.getInventory().addItem(clicked.clone());
            ShopStockView.open(player, shop);
            player.sendMessage(Util.color("&aRemoved listing and returned the item."));
            return;
        }
    }

    private void handleStockSelectionFromInventory(Player player, ItemStack clicked, int slot) {
        ShopManager manager = ShopManager.get();
        UUID shopId = manager.getActiveStockShop(player.getUniqueId());
        if (shopId == null) {
            player.sendMessage(Util.color("&cReopen the stock manager from your shop editor to add items."));
            return;
        }
        PlayerShop shop = manager.getShop(shopId);
        if (shop == null) {
            manager.clearActiveStockShop(player.getUniqueId());
            player.sendMessage(Util.color("&cThat shop no longer exists."));
            return;
        }

        ItemStack copy = clicked.clone();
        manager.setPendingItemAdd(player.getUniqueId(), shopId, copy, slot);
        manager.setPendingPrice(player.getUniqueId(), shopId);
        manager.setPendingManageReopen(player.getUniqueId(), ManageView.STOCK, shopId);
        player.closeInventory();
        player.sendMessage(Util.color("&eEnter the price for &f" + copy.getAmount() + "x &e" + Util.plainName(copy)
                + "&e in chat. Type &ccancel &eto abort."));
    }

    private int findMatchingSlot(PlayerInventory inventory, ItemStack target, int preferredSlot) {
        ItemStack preferred = inventory.getItem(preferredSlot);
        if (preferred != null && preferred.getType() != Material.AIR && preferred.isSimilar(target)
                && preferred.getAmount() >= target.getAmount()) {
            return preferredSlot;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack current = inventory.getItem(i);
            if (current != null && current.getType() != Material.AIR && current.isSimilar(target)
                    && current.getAmount() >= target.getAmount()) {
                return i;
            }
        }
        return -1;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ShopManager manager = ShopManager.get();
        boolean handled = false;

        if (manager.isRenaming(player.getUniqueId())) {
            handled = true;
            event.setCancelled(true);
            String message = ChatColor.stripColor(event.getMessage().trim());
            PlayerShop shop = manager.consumeRenameTarget(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(Util.color("&cRename cancelled."));
                reopenPendingManageView(player, shop);
                return;
            }
            if (shop != null) {
                shop.setName(message);
                manager.save();
                player.sendMessage(Util.color("&aShop name updated."));
            }
            reopenPendingManageView(player, shop);
        }

        if (manager.isDescribing(player.getUniqueId())) {
            handled = true;
            event.setCancelled(true);
            String message = ChatColor.stripColor(event.getMessage().trim());
            PlayerShop shop = manager.consumeDescriptionTarget(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(Util.color("&cDescription update cancelled."));
                reopenPendingManageView(player, shop);
                return;
            }
            if (shop != null) {
                shop.setDescription(message);
                manager.save();
                player.sendMessage(Util.color("&aShop description updated."));
            }
            reopenPendingManageView(player, shop);
        }

        if (manager.isAwaitingPrice(player.getUniqueId())) {
            handled = true;
            event.setCancelled(true);
            String message = ChatColor.stripColor(event.getMessage().trim());
            ShopManager.PendingItem pending = manager.consumePendingItem(player.getUniqueId());
            PlayerShop shop = pending == null ? null : manager.getShop(pending.shopId());
            if (message.equalsIgnoreCase("cancel")) {
                manager.consumePriceTarget(player.getUniqueId());
                player.sendMessage(Util.color("&cItem add cancelled."));
                reopenPendingManageView(player, shop);
                return;
            }
            double price;
            try {
                price = Double.parseDouble(message);
            } catch (NumberFormatException ex) {
                player.sendMessage(Util.color("&cEnter a valid number for the price or type cancel."));
                return;
            }
            manager.consumePriceTarget(player.getUniqueId());
            if (pending == null) {
                player.sendMessage(Util.color("&cNo pending item to add."));
                reopenPendingManageView(player, null);
                return;
            }
            if (shop == null) {
                player.sendMessage(Util.color("&cThat shop no longer exists."));
                reopenPendingManageView(player, null);
                return;
            }
            int slot = findMatchingSlot(player.getInventory(), pending.item(), pending.slot());
            if (slot == -1) {
                player.sendMessage(Util.color("&cCouldn't find the selected item in your inventory. Listing cancelled."));
                reopenPendingManageView(player, shop);
                return;
            }
            ItemStack inventoryItem = player.getInventory().getItem(slot);
            if (inventoryItem == null || inventoryItem.getAmount() < pending.item().getAmount()) {
                player.sendMessage(Util.color("&cYou no longer have enough of that item to list it."));
                reopenPendingManageView(player, shop);
                return;
            }
            boolean added = manager.addItemToShop(shop, pending.item(), price);
            if (!added) {
                player.sendMessage(Util.color("&cYour shop is full. Item not listed."));
            } else {
                int remaining = inventoryItem.getAmount() - pending.item().getAmount();
                if (remaining <= 0) {
                    player.getInventory().setItem(slot, new ItemStack(Material.AIR));
                } else {
                    inventoryItem.setAmount(remaining);
                }
                player.sendMessage(Util.color("&aItem added for &f" + price + "&a."));
            }
            reopenPendingManageView(player, shop);
        }

        if (handled) {
            manager.clearPendingPurchase(player.getUniqueId());
        }
    }

    private void reopenPendingManageView(Player player, PlayerShop shop) {
        HardcorePlugin plugin = HardcorePlugin.getInstance();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ShopManager manager = ShopManager.get();
            ManageReopen reopen = manager.consumePendingManageReopen(player.getUniqueId());
            if (reopen == null) {
                if (shop != null) {
                    ShopEditorView.open(player, shop);
                }
                return;
            }

            PlayerShop reopenShop = shop != null ? shop : manager.getShop(reopen.shopId());
            switch (reopen.view()) {
                case MANAGER -> ShopManagerView.open(player);
                case STOCK -> {
                    if (reopenShop != null) {
                        ShopStockView.open(player, reopenShop);
                    }
                }
                case EDITOR -> {
                    if (reopenShop != null) {
                        ShopEditorView.open(player, reopenShop);
                    }
                }
                default -> {
                }
            }
        });
    }
}
