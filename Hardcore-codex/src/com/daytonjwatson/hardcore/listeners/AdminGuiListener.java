package com.daytonjwatson.hardcore.listeners;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.AdminLogManager;
import com.daytonjwatson.hardcore.utils.Util;
import com.daytonjwatson.hardcore.views.AdminGui;

public class AdminGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        String title = view.getTitle();
        if (!title.equals(AdminGui.TITLE) && !title.equals(AdminGui.TITLE_PLAYER_PICKER)
                && !title.equals(AdminGui.TITLE_DURATION) && !title.equals(AdminGui.TITLE_BANK)
                && !title.equals(AdminGui.TITLE_AUCTION)) {
            return;
        }

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String action = AdminGui.getAction(current);
        if (action == null || action.isEmpty()) {
            return;
        }

        player.closeInventory();

        handleAction(player, action, current);
    }

    private void handleAction(Player player, String action, ItemStack current) {
        switch (action.toLowerCase(Locale.ROOT)) {
            case "open_main":
                AdminGui.open(player);
                return;
            case "select_ban":
                player.openInventory(AdminGui.playerPicker(player, "ban_select_target", AdminGui.gatherKnownPlayers(),
                        "No players found."));
                return;
            case "ban_select_target": {
                UUID target = AdminGui.getTarget(current);
                String name = AdminGui.getData(current);
                if (target != null && name != null) {
                    player.openInventory(AdminGui.durationPicker(player, target, name, "ban_with_duration"));
                }
                return;
            }
            case "ban_with_duration": {
                UUID target = AdminGui.getTarget(current);
                String name = AdminGui.getCommand(current);
                String token = AdminGui.getData(current);
                Duration duration = parseDuration(token);
                if (target == null || name == null || token == null) {
                    return;
                }
                String durationToken = duration == null ? "" : " " + token;
                String reason = duration == null ? "GUI ban" : "Banned for " + token;
                runCommand(player, "admin ban " + name + durationToken + " " + reason);
                return;
            }
            case "select_unban":
                player.openInventory(AdminGui.playerPicker(player, "unban_target", AdminGui.gatherBannedPlayers(),
                        "No banned players."));
                return;
            case "unban_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin unban " + name);
                }
                return;
            }
            case "select_mute":
                player.openInventory(AdminGui.playerPicker(player, "mute_target", AdminGui.gatherKnownPlayers(),
                        "No players found."));
                return;
            case "mute_target": {
                UUID target = AdminGui.getTarget(current);
                String name = AdminGui.getData(current);
                if (target != null && name != null) {
                    player.openInventory(AdminGui.durationPicker(player, target, name, "mute_with_duration"));
                }
                return;
            }
            case "mute_with_duration": {
                String name = AdminGui.getCommand(current);
                String duration = AdminGui.getData(current);
                if (name != null && duration != null) {
                    String reason = duration.equalsIgnoreCase("Perm") ? "Muted by admin" : "Muted for " + duration;
                    runCommand(player, "admin mute " + name + " " + duration + " " + reason);
                }
                return;
            }
            case "select_unmute":
                player.openInventory(AdminGui.playerPicker(player, "unmute_target", AdminGui.gatherMutedPlayers(),
                        "No muted players."));
                return;
            case "unmute_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin unmute " + name);
                }
                return;
            }
            case "select_warn":
                player.openInventory(AdminGui.playerPicker(player, "warn_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "warn_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin warn " + name + " GUI warning");
                }
                return;
            }
            case "select_kick":
                player.openInventory(AdminGui.playerPicker(player, "kick_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "kick_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin kick " + name + " Removed via GUI");
                }
                return;
            }
            case "select_freeze":
                player.openInventory(AdminGui.playerPicker(player, "freeze_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "freeze_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin freeze " + name + " Await staff review");
                }
                return;
            }
            case "select_unfreeze":
                player.openInventory(AdminGui.playerPicker(player, "unfreeze_target", AdminGui.gatherFrozenPlayers(),
                        "No frozen players."));
                return;
            case "unfreeze_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin unfreeze " + name);
                }
                return;
            }
            case "clear_chat":
                runCommand(player, "admin clearchat Moderation");
                return;
            case "open_help":
                runCommand(player, "admin help");
                return;
            case "show_logs":
                runCommand(player, "admin log 20");
                return;
            case "select_status":
                player.openInventory(AdminGui.playerPicker(player, "status_target", AdminGui.gatherKnownPlayers(),
                        "No players found."));
                return;
            case "status_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin status " + name);
                }
                return;
            }
            case "select_info":
                player.openInventory(AdminGui.playerPicker(player, "info_target", AdminGui.gatherKnownPlayers(),
                        "No players found."));
                return;
            case "info_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin info " + name);
                }
                return;
            }
            case "select_invsee":
                player.openInventory(AdminGui.playerPicker(player, "invsee_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "invsee_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin invsee " + name);
                }
                return;
            }
            case "select_endersee":
                player.openInventory(AdminGui.playerPicker(player, "endersee_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "endersee_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin endersee " + name);
                }
                return;
            }
            case "select_tp":
                player.openInventory(AdminGui.playerPicker(player, "tp_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "tp_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin tp " + name);
                }
                return;
            }
            case "select_tphere":
                player.openInventory(AdminGui.playerPicker(player, "tphere_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "tphere_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin tphere " + name);
                }
                return;
            }
            case "select_heal":
                player.openInventory(AdminGui.playerPicker(player, "heal_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "heal_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin heal " + name);
                }
                return;
            }
            case "select_feed":
                player.openInventory(AdminGui.playerPicker(player, "feed_target", AdminGui.gatherOnlinePlayers(),
                        "No online players."));
                return;
            case "feed_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin feed " + name);
                }
                return;
            }
            case "select_bank":
                player.openInventory(AdminGui.playerPicker(player, "bank_target", AdminGui.gatherKnownPlayers(),
                        "No players found."));
                return;
            case "bank_target": {
                UUID target = AdminGui.getTarget(current);
                if (target != null) {
                    player.openInventory(AdminGui.bankPicker(player, Bukkit.getOfflinePlayer(target)));
                }
                return;
            }
            case "bank_balance": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin bank " + name + " balance");
                }
                return;
            }
            case "bank_deposit":
            case "bank_withdraw":
            case "bank_set": {
                String name = AdminGui.getData(current);
                String amount = AdminGui.getCommand(current);
                if (name == null || amount == null) {
                    return;
                }
                runCommand(player, "admin bank " + name + " " + action.substring(5) + " " + amount);
                return;
            }
            case "open_auction":
                player.openInventory(AdminGui.auctionPanel(player));
                return;
            case "auction_list":
                runCommand(player, "admin auction list");
                return;
            case "auction_cancel": {
                String id = AdminGui.getData(current);
                if (id != null) {
                    runCommand(player, "admin auction cancel " + id + " Listing removed via GUI");
                }
                return;
            }
            case "select_add_admin":
                player.openInventory(AdminGui.playerPicker(player, "add_admin_target", AdminGui.gatherKnownPlayers(),
                        "No players found."));
                return;
            case "add_admin_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin add " + name);
                }
                return;
            }
            case "select_remove_admin":
                player.openInventory(AdminGui.playerPicker(player, "remove_admin_target", AdminGui.gatherAdmins(),
                        "No admins configured."));
                return;
            case "remove_admin_target": {
                String name = AdminGui.getData(current);
                if (name != null) {
                    runCommand(player, "admin remove " + name);
                }
                return;
            }
            case "list_admins":
                AdminLogManager.log(player, "Admin GUI list", true);
                player.sendMessage(Util.color("&6Admins: &e" + String.join(", ", AdminManager.getAdminNames())));
                return;
            case "execute_command": {
                String command = AdminGui.getCommand(current);
                if (command != null) {
                    runCommand(player, command.startsWith("/") ? command.substring(1) : command);
                }
                return;
            }
            default:
                player.sendMessage(Util.color("&cUnknown admin GUI action."));
        }
    }

    private Duration parseDuration(String token) {
        if (token == null) {
            return null;
        }
        token = token.toLowerCase(Locale.ROOT);
        if (token.startsWith("perm")) {
            return null;
        }
        try {
            long multiplier = 1000L;
            if (token.endsWith("h")) {
                multiplier *= 60 * 60;
                return Duration.ofMillis(Long.parseLong(token.replace("h", "")) * multiplier);
            }
            if (token.endsWith("d")) {
                multiplier *= 60 * 60 * 24;
                return Duration.ofMillis(Long.parseLong(token.replace("d", "")) * multiplier);
            }
            if (token.endsWith("m")) {
                multiplier *= 60;
                return Duration.ofMillis(Long.parseLong(token.replace("m", "")) * multiplier);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private void runCommand(Player player, String command) {
        AdminLogManager.log(player, "/" + command, true);
        player.performCommand(command.startsWith("/") ? command.substring(1) : command);
    }
}
