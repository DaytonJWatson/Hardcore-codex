package com.daytonjwatson.hardcore.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.daytonjwatson.hardcore.HardcorePlugin;
import com.daytonjwatson.hardcore.managers.AdminLogManager;
import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.AuctionHouseManager;
import com.daytonjwatson.hardcore.managers.BankManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.AdminGui;

public class AdminGuiListener implements Listener {

    private static final Map<UUID, PendingChat> pendingInputs = new HashMap<>();
    private static final org.bukkit.NamespacedKey TARGET_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_target");
    private static final org.bukkit.NamespacedKey PAGE_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_page");
    private static final org.bukkit.NamespacedKey ACTION_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_action");
    private static final org.bukkit.NamespacedKey DURATION_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_duration");
    private static final org.bukkit.NamespacedKey REASON_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_reason");
    private static final org.bukkit.NamespacedKey FILTER_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_filter");
    private static final org.bukkit.NamespacedKey STATUS_FILTER_KEY = new org.bukkit.NamespacedKey(
            HardcorePlugin.getInstance(), "admin_status_filter");
    private static final org.bukkit.NamespacedKey LISTING_KEY = new org.bukkit.NamespacedKey(HardcorePlugin.getInstance(),
            "admin_listing_id");

    private enum PendingType {
        BAN_REASON,
        CUSTOM_BAN,
        MUTE_REASON,
        CUSTOM_MUTE,
        WARN_REASON,
        KICK_REASON,
        BANK_DEPOSIT,
        BANK_WITHDRAW,
        BANK_SET,
        AUCTION_CANCEL,
        PLAYER_SEARCH,
        LIST_PLAYER_AUCTIONS,
        ADD_ADMIN,
        REMOVE_ADMIN,
        LOG_FILTER,
        BROADCAST
    }

    private record PendingChat(PendingType type, OfflinePlayer target, String extra) {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        boolean handled = handleClicks(event, title);
        if (handled) {
            event.setCancelled(true);
        }
    }

    private boolean handleClicks(InventoryClickEvent event, String title) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return false;
        }

        if (!title.equals(AdminGui.MAIN_TITLE) && !title.startsWith(AdminGui.PLAYER_LIST_TITLE)
                && !title.contains("Manage") && !title.equals(Util.color("&4&lAdmin &8| &7Choose Duration"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Choose Reason"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Choose Amount"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Bank Tools"))
                && !title.startsWith(Util.color("&4&lAdmin &8| &7Bank &ePlayer"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Bank &eHistory"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Auction Tools"))
                && !title.startsWith(Util.color("&4&lAdmin &8| &7Auction Sellers"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Auction Listings"))
                && !title.equals(AdminGui.LOG_TITLE)) {
            return false;
        }

        if (!hasAdminAccess(player)) {
            player.sendMessage(Util.color("&cYou must be a Hardcore admin to use this menu."));
            player.closeInventory();
            return true;
        }

        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return false;
        }
        event.setCancelled(true);

        String rawName = current.getItemMeta() != null ? current.getItemMeta().getDisplayName() : "";
        String plainName = ChatColor.stripColor(rawName == null ? "" : rawName);

        if (title.equals(AdminGui.MAIN_TITLE)) {
            return handleMainClick(player, plainName);
        }

        if (title.startsWith(AdminGui.PLAYER_LIST_TITLE)) {
            return handlePlayerListClick(player, current, plainName);
        }

        if (title.equals(AdminGui.LOG_TITLE)) {
            return handleLogClick(player, current, plainName);
        }

        if (title.contains("Manage")) {
            return handlePlayerActionClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Choose Duration"))) {
            return handleDurationClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Choose Reason"))) {
            return handleReasonClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Choose Amount"))) {
            return handleAmountClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Bank Tools"))) {
            return handleBankClick(player, current, plainName);
        }

        if (title.startsWith(Util.color("&4&lAdmin &8| &7Bank &ePlayer"))) {
            return handleBankPlayerClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Bank &eHistory"))) {
            return handleBankHistoryClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Auction Tools"))) {
            return handleAuctionClick(player, current, plainName);
        }

        if (title.startsWith(Util.color("&4&lAdmin &8| &7Auction Sellers"))) {
            return handleAuctionSellerClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Auction Listings"))) {
            return handleAuctionListingClick(player, current, plainName);
        }

        return false;
    }

    private boolean handleMainClick(Player player, String plainName) {
        switch (plainName.toLowerCase()) {
            case "manage players":
                AdminGui.openPlayerList(player, 0);
                return true;
            case "broadcast message":
                prompt(player, new PendingChat(PendingType.BROADCAST, null, null),
                        "&6Admin &8» &7Type the announcement to broadcast, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "switch to survival":
            case "switch to spectator":
                org.bukkit.GameMode next = player.getGameMode() == org.bukkit.GameMode.SPECTATOR
                        ? org.bukkit.GameMode.SURVIVAL
                        : org.bukkit.GameMode.SPECTATOR;
                player.setGameMode(next);
                player.sendMessage(Util.color("&6Admin &8» &7Gamemode set to &e" + next.name().toLowerCase()));
                AdminGui.openMain(player);
                return true;
            case "recent admin log":
                AdminGui.openAdminLog(player, null, null, 0);
                return true;
            case "clear chat":
                player.performCommand("admin clearchat");
                return true;
            case "auction tools":
                AdminGui.openAuctionMenu(player);
                return true;
            case "bank tools":
                AdminGui.openBankPlayerList(player, 0);
                return true;
            case "add admin":
                if (!player.isOp()) {
                    player.sendMessage(Util.color("&cYou must be an operator to add admins."));
                    return true;
                }
                prompt(player, new PendingChat(PendingType.ADD_ADMIN, null, null),
                        "&6Admin &8» &7Type the player to add as Hardcore admin, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "remove admin":
                if (!player.isOp()) {
                    player.sendMessage(Util.color("&cYou must be an operator to remove admins."));
                    return true;
                }
                prompt(player, new PendingChat(PendingType.REMOVE_ADMIN, null, null),
                        "&6Admin &8» &7Type the admin to remove, or &ccancel&7.");
                player.closeInventory();
                return true;
            default:
                return false;
        }
    }

    private boolean handlePlayerListClick(Player player, ItemStack current, String plainName) {
        ItemMeta meta = current.getItemMeta();
        if (plainName.equalsIgnoreCase("Back")) {
            AdminGui.openMain(player);
            return true;
        }

        if (plainName.toLowerCase().contains("previous")) {
            Integer page = getInt(meta, "admin_page");
            if (page != null) {
                AdminGui.openPlayerList(player, page);
                return true;
            }
        }

        if (plainName.toLowerCase().contains("next")) {
            Integer page = getInt(meta, "admin_page");
            if (page != null) {
                AdminGui.openPlayerList(player, page);
                return true;
            }
        }

        if (meta != null && meta.getPersistentDataContainer().has(TARGET_KEY, PersistentDataType.STRING)) {
            String targetId = meta.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
            Integer page = getInt(meta, "admin_page");
            if (targetId != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(targetId));
                AdminGui.openPlayerActions(player, target, page == null ? 0 : page);
                return true;
            }
        }

        return false;
    }

    private boolean handleLogClick(Player player, ItemStack current, String plainName) {
        ItemMeta meta = current.getItemMeta();
        String filter = getString(meta, FILTER_KEY);
        Boolean statusFilter = getStatus(meta);

        if (plainName.equalsIgnoreCase("Back")) {
            AdminGui.openMain(player);
            return true;
        }

        if (plainName.toLowerCase().contains("previous")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openAdminLog(player, filter, statusFilter, page == null ? 0 : page);
            return true;
        }

        if (plainName.toLowerCase().contains("next")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openAdminLog(player, filter, statusFilter, page == null ? 0 : page);
            return true;
        }

        if (current.getType() == org.bukkit.Material.HOPPER) {
            String nextFilter = cycleFilter(filter);
            AdminGui.openAdminLog(player, nextFilter, statusFilter, 0);
            return true;
        }

        if (current.getType() == org.bukkit.Material.REPEATER) {
            Boolean nextStatus = cycleStatus(statusFilter);
            AdminGui.openAdminLog(player, filter, nextStatus, 0);
            return true;
        }

        return false;
    }

    private boolean handlePlayerActionClick(Player player, ItemStack current, String plainName) {
        OfflinePlayer target = getTarget(current);
        if (target == null) {
            return false;
        }

        String name = safeName(target);

        switch (plainName.toLowerCase()) {
            case "info":
                player.performCommand("admin info " + name);
                return true;
            case "status":
                player.performCommand("admin status " + name);
                return true;
            case "inventory":
                player.performCommand("admin invsee " + name);
                return true;
            case "ender chest":
                player.performCommand("admin endersee " + name);
                return true;
            case "warn":
                AdminGui.openReasonMenu(player, target, "warn", null);
                return true;
            case "kick":
                AdminGui.openReasonMenu(player, target, "kick", null);
                return true;
            case "mute":
                pendingInputs.put(player.getUniqueId(), new PendingChat(PendingType.MUTE_REASON, target, null));
                AdminGui.openDurationMenu(player, target, "mute");
                return true;
            case "unmute":
                player.performCommand("admin unmute " + name);
                return true;
            case "ban":
                pendingInputs.put(player.getUniqueId(), new PendingChat(PendingType.BAN_REASON, target, null));
                AdminGui.openDurationMenu(player, target, "ban");
                return true;
            case "unban":
                player.performCommand("admin unban " + name);
                return true;
            case "freeze":
                player.performCommand("admin freeze " + name);
                return true;
            case "unfreeze":
                player.performCommand("admin unfreeze " + name);
                return true;
            case "teleport to":
                player.performCommand("admin tp " + name);
                return true;
            case "teleport here":
                player.performCommand("admin tphere " + name);
                return true;
            case "heal":
                player.performCommand("admin heal " + name);
                return true;
            case "feed":
                player.performCommand("admin feed " + name);
                return true;
            case "bank tools":
                AdminGui.openBankMenu(player, target);
                return true;
            case "auctions":
                AdminGui.openAuctionListings(player, target.getUniqueId(), 0);
                return true;
            case "back":
                ItemMeta meta = current.getItemMeta();
                Integer page = getInt(meta, "admin_page");
                AdminGui.openPlayerList(player, page == null ? 0 : page);
                return true;
            default:
                return false;
        }
    }

    private boolean handleDurationClick(Player player, ItemStack current, String plainName) {
        OfflinePlayer target = getTarget(current);
        if (target == null) {
            return false;
        }

        String lower = plainName.toLowerCase();
        String duration = null;
        if (lower.contains("10m")) {
            duration = "10m";
        } else if (lower.contains("1h")) {
            duration = "1h";
        } else if (lower.contains("1d")) {
            duration = "1d";
        } else if (lower.contains("permanent")) {
            duration = null;
        }

        if (lower.contains("back")) {
            pendingInputs.remove(player.getUniqueId());
            AdminGui.openPlayerActions(player, target);
            return true;
        }

        PendingType pendingType = pendingTypeFromTitle(player);
        if (lower.contains("custom")) {
            PendingType customType = pendingType == PendingType.MUTE_REASON ? PendingType.CUSTOM_MUTE : PendingType.CUSTOM_BAN;
            prompt(player, new PendingChat(customType, target, "custom"),
                    "&6Admin &8» &7Type a duration followed by a reason (e.g. &e30m griefing&7), or &ccancel&7.");
            player.closeInventory();
            return true;
        }

        if (pendingType == PendingType.BAN_REASON || pendingType == PendingType.MUTE_REASON) {
            AdminGui.openReasonMenu(player, target, pendingType == PendingType.BAN_REASON ? "ban" : "mute", duration);
            return true;
        }
        return false;
    }

    private boolean handleReasonClick(Player player, ItemStack current, String plainName) {
        OfflinePlayer target = getTarget(current);
        if (target == null) {
            return false;
        }

        ItemMeta meta = current.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (plainName.equalsIgnoreCase("Back")) {
            PendingChat pending = pendingInputs.get(player.getUniqueId());
            if (pending != null && (pending.type() == PendingType.BAN_REASON || pending.type() == PendingType.MUTE_REASON)) {
                AdminGui.openDurationMenu(player, target, pending.type() == PendingType.BAN_REASON ? "ban" : "mute");
            } else {
                AdminGui.openPlayerActions(player, target);
            }
            return true;
        }

        String action = meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        String duration = meta.getPersistentDataContainer().get(DURATION_KEY, PersistentDataType.STRING);
        String reasonKey = meta.getPersistentDataContainer().get(REASON_KEY, PersistentDataType.STRING);
        if (action == null || reasonKey == null) {
            return false;
        }

        if ("custom".equalsIgnoreCase(reasonKey)) {
            PendingType type = switch (action) {
                case "ban" -> PendingType.BAN_REASON;
                case "mute" -> PendingType.MUTE_REASON;
                case "kick" -> PendingType.KICK_REASON;
                default -> PendingType.WARN_REASON;
            };
            prompt(player, new PendingChat(type, target, duration),
                    "&6Admin &8» &7Type a custom reason for &e" + safeName(target) + "&7, or &ccancel&7.");
            player.closeInventory();
            return true;
        }

        String command = buildTimedCommand(action, target, duration, reasonKey);
        runCommand(player, command);
        pendingInputs.remove(player.getUniqueId());
        player.closeInventory();
        return true;
    }

    private boolean handleAmountClick(Player player, ItemStack current, String plainName) {
        OfflinePlayer target = getTarget(current);
        if (target == null) {
            return false;
        }

        ItemMeta meta = current.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (plainName.equalsIgnoreCase("Back")) {
            AdminGui.openBankMenu(player, target);
            return true;
        }

        String action = meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        String amountKey = meta.getPersistentDataContainer().get(DURATION_KEY, PersistentDataType.STRING);
        if (action == null) {
            return false;
        }

        if (amountKey == null) {
            PendingType type = switch (action.toLowerCase()) {
                case "deposit" -> PendingType.BANK_DEPOSIT;
                case "withdraw" -> PendingType.BANK_WITHDRAW;
                default -> PendingType.BANK_SET;
            };
            prompt(player, new PendingChat(type, target, null),
                    "&6Admin &8» &7Type the amount to " + action + " for &e" + safeName(target) + "&7, or &ccancel&7.");
            player.closeInventory();
            return true;
        }

        runCommand(player, "admin bank " + safeName(target) + " " + action + " " + amountKey);
        player.closeInventory();
        return true;
    }

    private PendingType pendingTypeFromTitle(Player player) {
        PendingChat pending = pendingInputs.get(player.getUniqueId());
        if (pending != null && (pending.type() == PendingType.BAN_REASON || pending.type() == PendingType.MUTE_REASON)) {
            return pending.type();
        }

        return PendingType.BAN_REASON;
    }

    private boolean handleBankClick(Player player, ItemStack current, String plainName) {
        OfflinePlayer target = getTarget(current);
        if (target == null) {
            return false;
        }

        switch (plainName.toLowerCase()) {
            case "view balance":
                player.performCommand("admin bank " + target.getName() + " balance");
                return true;
            case "deposit":
                AdminGui.openAmountMenu(player, target, "deposit");
                return true;
            case "withdraw":
                AdminGui.openAmountMenu(player, target, "withdraw");
                return true;
            case "set balance":
                AdminGui.openAmountMenu(player, target, "set");
                return true;
            case "recent transactions":
                AdminGui.openBankTransactions(player, target, 0);
                return true;
            case "suspicious activity":
                sendBankSummary(player, target);
                player.closeInventory();
                return true;
            case "back":
                Integer page = getInt(current.getItemMeta(), "admin_page");
                if (page != null) {
                    AdminGui.openBankPlayerList(player, page);
                } else {
                    AdminGui.openPlayerActions(player, target);
                }
                return true;
            default:
                return false;
        }
    }

    private boolean handleAuctionClick(Player player, ItemStack current, String plainName) {
        switch (plainName.toLowerCase()) {
            case "view all listings":
                AdminGui.openAuctionListings(player, null, 0);
                return true;
            case "filter by seller":
                AdminGui.openAuctionSellerList(player, 0);
                return true;
            case "command shortcuts":
                player.closeInventory();
                player.sendMessage(Util.color("&6Admin &8» &7Use &e/admin auction list &7or &e/admin auction cancel <id> [reason]&7."));
                return true;
            case "back":
                AdminGui.openMain(player);
                return true;
            default:
                return false;
        }
    }

    private boolean handleBankPlayerClick(Player player, ItemStack current, String plainName) {
        ItemMeta meta = current.getItemMeta();
        if (plainName.equalsIgnoreCase("Back")) {
            AdminGui.openMain(player);
            return true;
        }

        if (plainName.toLowerCase().contains("previous")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openBankPlayerList(player, page == null ? 0 : page);
            return true;
        }

        if (plainName.toLowerCase().contains("next")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openBankPlayerList(player, page == null ? 0 : page);
            return true;
        }

        if (current.getType() == org.bukkit.Material.COMPASS) {
            prompt(player, new PendingChat(PendingType.PLAYER_SEARCH, null, "bank"),
                    "&6Admin &8» &7Type the player to manage bank tools, or &ccancel&7.");
            player.closeInventory();
            return true;
        }

        if (meta != null && meta.getPersistentDataContainer().has(TARGET_KEY, PersistentDataType.STRING)) {
            String id = meta.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
            Integer page = getInt(meta, "admin_page");
            if (id != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(id));
                AdminGui.openBankMenu(player, target, page);
                return true;
            }
        }
        return false;
    }

    private boolean handleBankHistoryClick(Player player, ItemStack current, String plainName) {
        ItemMeta meta = current.getItemMeta();
        OfflinePlayer target = getTarget(current);
        if (target == null) {
            return false;
        }

        if (plainName.equalsIgnoreCase("Back")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openBankMenu(player, target, page);
            return true;
        }

        if (plainName.toLowerCase().contains("previous")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openBankTransactions(player, target, page == null ? 0 : page);
            return true;
        }

        if (plainName.toLowerCase().contains("next")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openBankTransactions(player, target, page == null ? 0 : page);
            return true;
        }
        return false;
    }

    private boolean handleAuctionSellerClick(Player player, ItemStack current, String plainName) {
        ItemMeta meta = current.getItemMeta();
        if (plainName.equalsIgnoreCase("Back")) {
            AdminGui.openAuctionMenu(player);
            return true;
        }

        if (plainName.toLowerCase().contains("previous")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openAuctionSellerList(player, page == null ? 0 : page);
            return true;
        }

        if (plainName.toLowerCase().contains("next")) {
            Integer page = getInt(meta, "admin_page");
            AdminGui.openAuctionSellerList(player, page == null ? 0 : page);
            return true;
        }

        if (meta != null && meta.getPersistentDataContainer().has(TARGET_KEY, PersistentDataType.STRING)) {
            String id = meta.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
            if (id != null) {
                AdminGui.openAuctionListings(player, UUID.fromString(id), 0);
                return true;
            }
        }
        return false;
    }

    private boolean handleAuctionListingClick(Player player, ItemStack current, String plainName) {
        ItemMeta meta = current.getItemMeta();
        UUID sellerFilter = null;
        Integer page = null;
        if (meta != null) {
            String target = meta.getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
            if (target != null) {
                sellerFilter = UUID.fromString(target);
            }
            page = getInt(meta, "admin_page");
        }

        if (plainName.equalsIgnoreCase("Back")) {
            if (sellerFilter != null) {
                AdminGui.openAuctionSellerList(player, page == null ? 0 : page);
            } else {
                AdminGui.openAuctionMenu(player);
            }
            return true;
        }

        if (plainName.toLowerCase().contains("previous")) {
            AdminGui.openAuctionListings(player, sellerFilter, page == null ? 0 : page);
            return true;
        }

        if (plainName.toLowerCase().contains("next")) {
            AdminGui.openAuctionListings(player, sellerFilter, page == null ? 0 : page);
            return true;
        }

        if (meta != null && meta.getPersistentDataContainer().has(LISTING_KEY, PersistentDataType.STRING)) {
            String rawId = meta.getPersistentDataContainer().get(LISTING_KEY, PersistentDataType.STRING);
            if (rawId != null) {
                UUID id = UUID.fromString(rawId);
                AuctionHouseManager manager = AuctionHouseManager.get();
                if (manager == null) {
                    player.sendMessage(Util.color("&cThe auction house is not initialized."));
                    return true;
                }
                boolean cancelled = manager.cancelListing(id, player.getName(), "Removed via admin GUI.");
                if (cancelled) {
                    player.sendMessage(Util.color("&aCancelled auction listing &f" + id + "&a."));
                } else {
                    player.sendMessage(Util.color("&cThat listing no longer exists."));
                }
                AdminGui.openAuctionListings(player, sellerFilter, page == null ? 0 : page);
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingChat pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) {
            return;
        }

        if (!hasAdminAccess(player)) {
            pendingInputs.remove(player.getUniqueId());
            player.sendMessage(Util.color("&cYou are no longer allowed to perform admin actions."));
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(player.getUniqueId());
            player.sendMessage(Util.color("&cAction cancelled."));
            return;
        }

        switch (pending.type()) {
            case WARN_REASON:
                runCommand(player, "admin warn " + safeName(pending.target()) + " " + message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case KICK_REASON:
                runCommand(player, "admin kick " + safeName(pending.target()) + " " + message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case BAN_REASON:
                runCommand(player, buildTimedCommand("ban", pending.target(), pending.extra(), message));
                pendingInputs.remove(player.getUniqueId());
                return;
            case MUTE_REASON:
                runCommand(player, buildTimedCommand("mute", pending.target(), pending.extra(), message));
                pendingInputs.remove(player.getUniqueId());
                return;
            case CUSTOM_BAN:
                handleCustomTimed(player, pending.target(), message, "ban");
                return;
            case CUSTOM_MUTE:
                handleCustomTimed(player, pending.target(), message, "mute");
                return;
            case BANK_DEPOSIT:
                if (handleAmount(player, pending.target(), message, "deposit")) {
                    pendingInputs.remove(player.getUniqueId());
                }
                return;
            case BANK_WITHDRAW:
                if (handleAmount(player, pending.target(), message, "withdraw")) {
                    pendingInputs.remove(player.getUniqueId());
                }
                return;
            case BANK_SET:
                if (handleAmount(player, pending.target(), message, "set")) {
                    pendingInputs.remove(player.getUniqueId());
                }
                return;
            case PLAYER_SEARCH:
                openTargetFromSearch(player, message, pending.extra());
                pendingInputs.remove(player.getUniqueId());
                return;
            case LIST_PLAYER_AUCTIONS:
                runCommand(player, "admin auction list " + message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case AUCTION_CANCEL:
                handleAuctionCancel(player, message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case ADD_ADMIN:
                runCommand(player, "admin add " + message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case REMOVE_ADMIN:
                runCommand(player, "admin remove " + message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case LOG_FILTER:
                runCommand(player, "admin log " + message);
                pendingInputs.remove(player.getUniqueId());
                return;
            case BROADCAST:
                AdminLogManager.log(player, "/admin broadcast " + message, true);
                Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(),
                        () -> Bukkit.broadcastMessage(Util.color("&6[Admin] &f" + message)));
                pendingInputs.remove(player.getUniqueId());
                return;
            default:
                break;
        }
    }

    private void handleAuctionCancel(Player player, String message) {
        String[] parts = message.split(" ", 2);
        try {
            UUID.fromString(parts[0]);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Util.color("&cThat is not a valid listing id."));
            return;
        }

        String reason = parts.length > 1 ? parts[1] : "Listing removed by an admin.";
        runCommand(player, "admin auction cancel " + parts[0] + " " + reason);
    }

    private void sendBankSummary(Player player, OfflinePlayer target) {
        List<String> transactions = BankManager.get().getTransactions(target.getUniqueId());
        double net5 = netChange(transactions, 5);
        double net15 = netChange(transactions, 15);
        double peakDeposit = peakMagnitude(transactions, 20, true);
        double peakWithdraw = peakMagnitude(transactions, 20, false);

        player.sendMessage(Util.color("&6Bank &8» &7Audit for &e" + safeName(target)));
        player.sendMessage(Util.color("&7Net last 5: " + formatCurrency(net5)));
        player.sendMessage(Util.color("&7Net last 15: " + formatCurrency(net15)));
        player.sendMessage(Util.color("&7Largest deposit (last 20): " + formatCurrency(peakDeposit)));
        player.sendMessage(Util.color("&7Largest withdraw (last 20): " + formatCurrency(-peakWithdraw)));
    }

    private void handleCustomTimed(Player player, OfflinePlayer target, String message, String verb) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            player.sendMessage(Util.color("&cPlease include a duration and reason (e.g. 45m spamming)."));
            return;
        }
        runCommand(player, "admin " + verb + " " + safeName(target) + " " + parts[0] + " " + parts[1]);
        pendingInputs.remove(player.getUniqueId());
    }

    private double netChange(List<String> transactions, int limit) {
        double total = 0;
        int count = Math.min(limit, transactions.size());
        for (int i = 0; i < count; i++) {
            total += parseAmount(transactions.get(i));
        }
        return total;
    }

    private double peakMagnitude(List<String> transactions, int limit, boolean deposit) {
        double peak = 0;
        int count = Math.min(limit, transactions.size());
        for (int i = 0; i < count; i++) {
            double amount = parseAmount(transactions.get(i));
            if (deposit) {
                if (amount > peak) {
                    peak = amount;
                }
            } else {
                if (amount < peak) {
                    peak = amount;
                }
            }
        }
        return peak;
    }

    private double parseAmount(String entry) {
        int bracket = entry.indexOf(']');
        if (bracket == -1 || bracket + 2 >= entry.length()) {
            return 0;
        }

        String body = entry.substring(bracket + 2).trim();
        if (body.isEmpty()) {
            return 0;
        }

        int sign = body.startsWith("-") ? -1 : 1;
        String digits = body.substring(1).replaceAll("[^0-9.]", "");
        try {
            return sign * Double.parseDouble(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String formatCurrency(double amount) {
        String formatted = BankManager.get().formatCurrency(Math.abs(amount));
        if (amount > 0) {
            return Util.color("&a+" + formatted);
        }
        if (amount < 0) {
            return Util.color("&c-" + formatted);
        }
        return Util.color("&7" + formatted);
    }

    private boolean handleAmount(Player player, OfflinePlayer target, String message, String action) {
        double amount;
        try {
            amount = Double.parseDouble(message);
        } catch (NumberFormatException ex) {
            player.sendMessage(Util.color("&cPlease type a valid number or &ccancel&c."));
            return false;
        }
        if (amount < 0) {
            player.sendMessage(Util.color("&cAmount cannot be negative."));
            return false;
        }
        runCommand(player, "admin bank " + safeName(target) + " " + action + " " + amount);
        return true;
    }

    private void openTargetFromSearch(Player player, String name, String extra) {
        OfflinePlayer target = findPlayerByName(name);
        if (target == null || target.getName() == null) {
            player.sendMessage(Util.color("&cCould not find player &f" + name + "&c."));
            return;
        }

        if ("bank".equalsIgnoreCase(extra)) {
            Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(),
                    () -> AdminGui.openBankMenu(player, target));
        } else {
            Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(),
                    () -> AdminGui.openPlayerActions(player, target));
        }
    }

    private OfflinePlayer getTarget(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(TARGET_KEY, PersistentDataType.STRING);
        if (id == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(UUID.fromString(id));
    }

    private OfflinePlayer findPlayerByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private Integer getInt(ItemMeta meta, String key) {
        if (meta == null) {
            return null;
        }
        NamespacedKey namespacedKey = "admin_page".equals(key) ? PAGE_KEY
                : new NamespacedKey(HardcorePlugin.getInstance(), key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
    }

    private Integer getInt(ItemMeta meta, NamespacedKey key) {
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
    }

    private String getString(ItemMeta meta, NamespacedKey key) {
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private Boolean getStatus(ItemMeta meta) {
        Integer stored = getInt(meta, STATUS_FILTER_KEY);
        if (stored == null) {
            return null;
        }
        return stored > 0;
    }

    private Boolean cycleStatus(Boolean current) {
        if (current == null) {
            return Boolean.TRUE;
        }
        if (current) {
            return Boolean.FALSE;
        }
        return null;
    }

    private String cycleFilter(String current) {
        List<String> actors = new ArrayList<>(AdminLogManager.getKnownActors());
        actors.sort(String.CASE_INSENSITIVE_ORDER);

        if (actors.isEmpty()) {
            return null;
        }

        actors.add(0, "All");
        String active = current == null ? "All" : current;
        int index = actors.indexOf(active);
        int nextIndex = (index + 1) % actors.size();
        String next = actors.get(nextIndex);
        return "All".equalsIgnoreCase(next) ? null : next;
    }

    private void prompt(Player player, PendingChat pending, String message) {
        pendingInputs.put(player.getUniqueId(), pending);
        player.sendMessage(Util.color(message));
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() == null ? "Unknown" : player.getName();
    }

    private String buildTimedCommand(String verb, OfflinePlayer target, String durationKey, String reason) {
        if (durationKey == null) {
            return "admin " + verb + " " + safeName(target) + " " + reason;
        }
        return "admin " + verb + " " + safeName(target) + " " + durationKey + " " + reason;
    }

    private void runCommand(Player player, String command) {
        Bukkit.getScheduler().runTask(HardcorePlugin.getInstance(), () -> player.performCommand(command));
    }

    private boolean hasAdminAccess(Player player) {
        return player.hasPermission("hardcore.admin") || player.isOp() || AdminManager.isAdmin(player);
    }
}
