package com.daytonjwatson.hardcore.listeners;

import java.util.HashMap;
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
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.AdminGui;

public class AdminGuiListener implements Listener {

    private static final Map<UUID, PendingChat> pendingInputs = new HashMap<>();
    private static final NamespacedKey TARGET_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_target");
    private static final NamespacedKey PAGE_KEY = new NamespacedKey(HardcorePlugin.getInstance(), "admin_page");

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
        REMOVE_ADMIN
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
                && !title.equals(Util.color("&4&lAdmin &8| &7Bank Tools"))
                && !title.equals(Util.color("&4&lAdmin &8| &7Auction Tools"))) {
            return false;
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

        if (title.contains("Manage")) {
            return handlePlayerActionClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Choose Duration"))) {
            return handleDurationClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Bank Tools"))) {
            return handleBankClick(player, current, plainName);
        }

        if (title.equals(Util.color("&4&lAdmin &8| &7Auction Tools"))) {
            return handleAuctionClick(player, plainName);
        }

        return false;
    }

    private boolean handleMainClick(Player player, String plainName) {
        switch (plainName.toLowerCase()) {
            case "manage players":
                AdminGui.openPlayerList(player, 0);
                return true;
            case "search player":
                prompt(player, new PendingChat(PendingType.PLAYER_SEARCH, null, null),
                        "&6Admin &8» &7Type a player name to open their panel, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "admin list":
                player.performCommand("admin list");
                return true;
            case "recent admin log":
                player.performCommand("admin log");
                return true;
            case "clear chat":
                player.performCommand("admin clearchat");
                return true;
            case "auction tools":
                AdminGui.openAuctionMenu(player);
                return true;
            case "bank tools":
                prompt(player, new PendingChat(PendingType.PLAYER_SEARCH, null, "bank"),
                        "&6Admin &8» &7Type a player name to open bank tools, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "add admin":
                prompt(player, new PendingChat(PendingType.ADD_ADMIN, null, null),
                        "&6Admin &8» &7Type the player to add as Hardcore admin, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "remove admin":
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
            if (targetId != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(targetId));
                AdminGui.openPlayerActions(player, target);
                return true;
            }
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
                prompt(player, new PendingChat(PendingType.WARN_REASON, target, null),
                        "&6Admin &8» &7Type a warning message for &e" + safeName(target)
                                + "&7, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "kick":
                prompt(player, new PendingChat(PendingType.KICK_REASON, target, null),
                        "&6Admin &8» &7Type a kick reason for &e" + safeName(target) + "&7, or &ccancel&7.");
                player.closeInventory();
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
                player.performCommand("admin auction list " + name);
                return true;
            case "back":
                AdminGui.openPlayerList(player, 0);
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
            prompt(player, new PendingChat(pendingType, target, duration),
                    "&6Admin &8» &7Type a reason for &e" + safeName(target) + "&7, or &ccancel&7.");
            player.closeInventory();
            return true;
        }
        return false;
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
                prompt(player, new PendingChat(PendingType.BANK_DEPOSIT, target, null),
                        "&6Admin &8» &7Type an amount to deposit into &e" + safeName(target) + "&7, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "withdraw":
                prompt(player, new PendingChat(PendingType.BANK_WITHDRAW, target, null),
                        "&6Admin &8» &7Type an amount to withdraw from &e" + safeName(target) + "&7, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "set balance":
                prompt(player, new PendingChat(PendingType.BANK_SET, target, null),
                        "&6Admin &8» &7Type the new balance for &e" + safeName(target) + "&7, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "back":
                AdminGui.openPlayerActions(player, target);
                return true;
            default:
                return false;
        }
    }

    private boolean handleAuctionClick(Player player, String plainName) {
        switch (plainName.toLowerCase()) {
            case "list all":
                player.performCommand("admin auction list");
                return true;
            case "list by player":
                prompt(player, new PendingChat(PendingType.LIST_PLAYER_AUCTIONS, null, null),
                        "&6Admin &8» &7Type a player name to filter auctions, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "cancel listing":
                prompt(player, new PendingChat(PendingType.AUCTION_CANCEL, null, null),
                        "&6Admin &8» &7Type the listing id and optional reason, or &ccancel&7.");
                player.closeInventory();
                return true;
            case "back":
                AdminGui.openMain(player);
                return true;
            default:
                return false;
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingChat pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) {
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

    private void handleCustomTimed(Player player, OfflinePlayer target, String message, String verb) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            player.sendMessage(Util.color("&cPlease include a duration and reason (e.g. 45m spamming)."));
            return;
        }
        runCommand(player, "admin " + verb + " " + safeName(target) + " " + parts[0] + " " + parts[1]);
        pendingInputs.remove(player.getUniqueId());
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
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getName() == null) {
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

    private Integer getInt(ItemMeta meta, String key) {
        if (meta == null) {
            return null;
        }
        NamespacedKey namespacedKey = "admin_page".equals(key) ? PAGE_KEY
                : new NamespacedKey(HardcorePlugin.getInstance(), key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
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
}
