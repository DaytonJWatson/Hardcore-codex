package com.daytonjwatson.hardcore.commands;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.AdminLogManager;
import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.FreezeManager;
import com.daytonjwatson.hardcore.managers.MuteManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> BASE_SUBCOMMANDS = Arrays.asList(
            "help", "add", "remove", "list", "ban", "unban", "kick", "mute", "unmute", "warn",
            "freeze", "unfreeze", "clearchat", "status", "info", "invsee", "endersee", "tp",
            "tphere", "heal", "feed", "log");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean bootstrapAllowed = !AdminManager.hasAdmins()
                && (sender.hasPermission("hardcore.admin") || sender.isOp() || sender instanceof ConsoleCommandSender);
        boolean isAdmin = AdminManager.isAdmin(sender);
        boolean isOp = sender.isOp() || sender instanceof ConsoleCommandSender;
        boolean canUseAdminTools = bootstrapAllowed || isAdmin;

        String fullCommand = "/" + label + (args.length > 0 ? " " + String.join(" ", args) : "");

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (!canUseAdminTools && !isOp) {
                AdminLogManager.log(sender, fullCommand, false);
                sender.sendMessage(Util.color("&cYou must be a Hardcore admin to use this command."));
                return true;
            }
            AdminLogManager.log(sender, fullCommand, true);
            sendAdminHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("add")) {
            handleAdd(sender, args, isOp);
            return true;
        }

        if (!canUseAdminTools) {
            AdminLogManager.log(sender, fullCommand, false);
            sender.sendMessage(Util.color("&cYou must be a Hardcore admin to use this command."));
            return true;
        }

        switch (sub) {
            case "add":
                // handled earlier
                break;
            case "remove":
                AdminLogManager.log(sender, fullCommand, true);
                handleRemove(sender, args);
                break;
            case "list":
                AdminLogManager.log(sender, fullCommand, true);
                handleList(sender);
                break;
            case "ban":
                AdminLogManager.log(sender, fullCommand, true);
                handleBan(sender, args);
                break;
            case "unban":
                AdminLogManager.log(sender, fullCommand, true);
                handleUnban(sender, args);
                break;
            case "kick":
                AdminLogManager.log(sender, fullCommand, true);
                handleKick(sender, args);
                break;
            case "mute":
                AdminLogManager.log(sender, fullCommand, true);
                handleMute(sender, args);
                break;
            case "unmute":
                AdminLogManager.log(sender, fullCommand, true);
                handleUnmute(sender, args);
                break;
            case "warn":
                AdminLogManager.log(sender, fullCommand, true);
                handleWarn(sender, args);
                break;
            case "freeze":
                AdminLogManager.log(sender, fullCommand, true);
                handleFreeze(sender, args);
                break;
            case "unfreeze":
                AdminLogManager.log(sender, fullCommand, true);
                handleUnfreeze(sender, args);
                break;
            case "clearchat":
                AdminLogManager.log(sender, fullCommand, true);
                handleClearChat(sender, args);
                break;
            case "status":
                AdminLogManager.log(sender, fullCommand, true);
                handleStatus(sender, args);
                break;
            case "log":
                AdminLogManager.log(sender, fullCommand, true);
                handleLog(sender, args);
                break;
            case "info":
                AdminLogManager.log(sender, fullCommand, true);
                handleInfo(sender, args);
                break;
            case "invsee":
                AdminLogManager.log(sender, fullCommand, true);
                handleInvSee(sender, args);
                break;
            case "endersee":
                AdminLogManager.log(sender, fullCommand, true);
                handleEnderSee(sender, args);
                break;
            case "tp":
                AdminLogManager.log(sender, fullCommand, true);
                handleTeleportTo(sender, args);
                break;
            case "tphere":
                AdminLogManager.log(sender, fullCommand, true);
                handleTeleportHere(sender, args);
                break;
            case "heal":
                AdminLogManager.log(sender, fullCommand, true);
                handleHeal(sender, args);
                break;
            case "feed":
                AdminLogManager.log(sender, fullCommand, true);
                handleFeed(sender, args);
                break;
            default:
                AdminLogManager.log(sender, fullCommand, false);
                sendAdminHelp(sender, label);
                break;
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args, boolean isOp) {
        if (!isOp) {
            AdminLogManager.log(sender, "/admin add" + (args.length > 1 ? " " + args[1] : ""), false);
            sender.sendMessage(Util.color("&cOnly server operators or console can add Hardcore admins."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin add <player>"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        boolean added = AdminManager.addAdmin(target);
        AdminLogManager.log(sender, "/admin add " + targetName, true);
        if (added) {
            sender.sendMessage(Util.color("&aAdded &e" + targetName + " &ato Hardcore admin list."));
        } else {
            sender.sendMessage(Util.color("&e" + targetName + " &cis already an admin."));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin remove <player>"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        boolean removed = AdminManager.removeAdmin(target);
        if (removed) {
            sender.sendMessage(Util.color("&aRemoved &e" + targetName + " &afrom Hardcore admin list."));
        } else {
            sender.sendMessage(Util.color("&e" + targetName + " &cis not an admin."));
        }
    }

    private void handleList(CommandSender sender) {
        List<String> admins = AdminManager.getAdminLabels();
        if (admins.isEmpty()) {
            sender.sendMessage(Util.color("&eNo admins are configured yet."));
            return;
        }

        MessageStyler.sendPanel(sender, "Admin Team", admins.toArray(new String[0]));
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin ban <player> [duration] [reason]"));
            return;
        }

        Duration duration = null;
        String reason;

        if (args.length >= 3) {
            Duration parsed = parseDuration(args[2]);
            if (parsed != null) {
                duration = parsed;
                reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : "Banned for " + Util.formatDuration(duration.toMillis());
            } else {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        } else {
            reason = "Banned by Hardcore admin.";
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        boolean wasBanned = BanManager.isBanned(target.getUniqueId());

        BanManager.ban(target, reason, duration, sender.getName());

        if (target.isOnline()) {
            ((Player) target).kickPlayer(Util.color("&cYou have been banned "
                    + formatDurationText(duration) + ". Reason: &7" + reason));
        }

        String durationText = formatDurationText(duration);
        Bukkit.broadcast(Util.color("&4&l[ADMIN]&c " + targetName + " was banned " + durationText + ": &7" + reason),
                "hardcore.admin");
        sender.sendMessage(Util.color("&a" + (wasBanned ? "Updated ban for " : "Banned ") + "&e" + targetName
                + " &a" + durationText + "."));
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin unban <player>"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();

        boolean success = BanManager.unban(target.getUniqueId());
        if (success) {
            sender.sendMessage(Util.color("&aUnbanned &e" + targetName + "&a."));
        } else {
            sender.sendMessage(Util.color("&e" + targetName + " &cis not currently banned."));
        }
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin kick <player> [reason]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer not online."));
            return;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "Kicked by Hardcore admin.";
        target.kickPlayer(Util.color("&cYou have been kicked. Reason: &7" + reason));
        sender.sendMessage(Util.color("&aKicked &e" + target.getName() + "&a."));
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin mute <player> [duration] [reason]"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        Duration duration = null;
        String reason;

        if (args.length >= 3) {
            Duration parsed = parseDuration(args[2]);
            if (parsed != null) {
                duration = parsed;
                reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : "Muted for " + Util.formatDuration(duration.toMillis());
            } else {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        } else {
            reason = "Muted by Hardcore admin.";
        }

        MuteManager.mute(target, reason, duration, sender.getName());
        if (target.isOnline()) {
            Player online = (Player) target;
            online.playSound(online.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
            online.sendMessage(Util.color("&cYou have been muted. Reason: &7" + reason));
        }

        sender.sendMessage(Util.color("&aMuted &e" + targetName + "&a."));
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin unmute <player>"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        boolean success = MuteManager.unmute(target.getUniqueId());
        if (success) {
            sender.sendMessage(Util.color("&aUnmuted &e" + targetName + "&a."));
        } else {
            sender.sendMessage(Util.color("&e" + targetName + " &cis not muted."));
        }
    }

    private void handleWarn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin warn <player> [reason]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer must be online to warn."));
            return;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "Rule reminder from the admin team.";

        target.sendTitle(Util.color("&c&lWarning"), Util.color("&e" + reason), 10, 70, 20);
        target.sendMessage(Util.color("&cYou received a warning: &e" + reason));
        sender.sendMessage(Util.color("&aWarned &e" + target.getName() + " &afor: &7" + reason));
        Bukkit.broadcast(Util.color("&4&l[ADMIN]&c " + target.getName() + " was warned: &7" + reason), "hardcore.admin");
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin freeze <player> [reason]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer must be online to freeze."));
            return;
        }

        if (FreezeManager.isFrozen(target.getUniqueId())) {
            sender.sendMessage(Util.color("&e" + target.getName() + " &cis already frozen."));
            return;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "Please wait for admin review.";

        FreezeManager.freeze(target, reason);
        target.sendMessage(Util.color("&cYou have been frozen by an admin. &7Reason: &e" + reason));
        sender.sendMessage(Util.color("&aFrozen &e" + target.getName() + " &afor: &7" + reason));
        Bukkit.broadcast(Util.color("&4&l[ADMIN]&c " + target.getName() + " was frozen: &7" + reason), "hardcore.admin");
    }

    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin unfreeze <player>"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        boolean unfrozen = FreezeManager.unfreeze(target.getUniqueId());
        if (unfrozen) {
            sender.sendMessage(Util.color("&aUnfroze &e" + (target.getName() == null ? args[1] : target.getName()) + "&a."));
        } else {
            sender.sendMessage(Util.color("&e" + (target.getName() == null ? args[1] : target.getName()) + " &cis not frozen."));
        }
    }

    private void handleClearChat(CommandSender sender, String[] args) {
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Chat moderation";
        String filler = " ".repeat(2);
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 80; i++) {
                player.sendMessage(filler);
            }
            player.sendMessage(Util.color("&7Chat was cleared by an admin. &fReason: &e" + reason));
        }
        sender.sendMessage(Util.color("&aCleared chat for all players."));
    }

    private void handleStatus(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin status <player>"));
            return;
        }

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();
        boolean isMuted = MuteManager.isMuted(uuid);
        boolean isBanned = BanManager.isBanned(uuid);
        boolean isAdmin = AdminManager.isAdmin(uuid);
        boolean isFrozen = FreezeManager.isFrozen(uuid);

        List<String> lines = new ArrayList<>();
        lines.add(MessageStyler.bulletLine("Role", org.bukkit.ChatColor.GOLD, isAdmin ? "Admin" : "Player"));
        if (isMuted) {
            long remaining = MuteManager.getRemainingMillis(uuid);
            String duration = remaining == -1L ? "Permanent" : Util.formatDuration(remaining);
            lines.add(MessageStyler.bulletLine("Mute", org.bukkit.ChatColor.RED,
                    "Muted for " + duration + " - " + MuteManager.getReason(uuid)));
        } else {
            lines.add(MessageStyler.bulletLine("Mute", org.bukkit.ChatColor.GREEN, "Not muted"));
        }

        if (isBanned) {
            long remaining = BanManager.getRemainingMillis(uuid);
            String duration = remaining == -1L ? "Permanent" : Util.formatDuration(remaining);
            lines.add(MessageStyler.bulletLine("Ban", org.bukkit.ChatColor.RED,
                    "Banned for " + duration + " - " + BanManager.getReason(uuid)));
        } else {
            lines.add(MessageStyler.bulletLine("Ban", org.bukkit.ChatColor.GREEN, "Not banned"));
        }

        if (isFrozen) {
            lines.add(MessageStyler.bulletLine("Freeze", org.bukkit.ChatColor.RED,
                    "Frozen - " + FreezeManager.getReason(uuid)));
        } else {
            lines.add(MessageStyler.bulletLine("Freeze", org.bukkit.ChatColor.GREEN, "Not frozen"));
        }

        MessageStyler.sendPanel(sender, target.getName() + " status", lines.toArray(new String[0]));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin info <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer must be online to view connection info."));
            return;
        }

        String ip = target.getAddress() != null && target.getAddress().getAddress() != null
                ? target.getAddress().getAddress().getHostAddress()
                : "Unknown";

        List<String> lines = new ArrayList<>();
        lines.add(MessageStyler.bulletLine("Name", org.bukkit.ChatColor.GOLD, target.getName()));
        lines.add(MessageStyler.bulletLine("UUID", org.bukkit.ChatColor.AQUA, target.getUniqueId().toString()));
        lines.add(MessageStyler.bulletLine("IP", org.bukkit.ChatColor.YELLOW, ip));
        lines.add(MessageStyler.bulletLine("World", org.bukkit.ChatColor.GREEN, target.getWorld().getName()));
        lines.add(MessageStyler.bulletLine("Coords", org.bukkit.ChatColor.GREEN,
                String.format("%.1f, %.1f, %.1f", target.getLocation().getX(), target.getLocation().getY(),
                        target.getLocation().getZ())));
        AttributeInstance maxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : target.getHealth();
        lines.add(MessageStyler.bulletLine("Health", org.bukkit.ChatColor.RED,
                String.format("%.1f / %.1f", target.getHealth(), maxHealth)));
        lines.add(MessageStyler.bulletLine("Food", org.bukkit.ChatColor.GOLD,
                target.getFoodLevel() + " (sat: " + String.format("%.1f", target.getSaturation()) + ")"));
        lines.add(MessageStyler.bulletLine("Gamemode", org.bukkit.ChatColor.AQUA, target.getGameMode().name()));
        lines.add(MessageStyler.bulletLine("Flight", org.bukkit.ChatColor.LIGHT_PURPLE,
                target.getAllowFlight() ? "Allowed" : "Not allowed"));
        lines.add(MessageStyler.bulletLine("Admin", org.bukkit.ChatColor.YELLOW,
                AdminManager.isAdmin(target) ? "Yes" : "No"));
        lines.add(MessageStyler.bulletLine("Muted", org.bukkit.ChatColor.YELLOW,
                MuteManager.isMuted(target.getUniqueId()) ? "Yes" : "No"));
        lines.add(MessageStyler.bulletLine("Banned", org.bukkit.ChatColor.YELLOW,
                BanManager.isBanned(target.getUniqueId()) ? "Yes" : "No"));

        MessageStyler.sendPanel(sender, target.getName() + " info", lines.toArray(new String[0]));
    }

    private void handleInvSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Util.color("&cOnly in-game admins can inspect inventories."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin invsee <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer not online."));
            return;
        }

        viewer.openInventory(target.getInventory());
        viewer.sendMessage(Util.color("&aInspecting &e" + target.getName() + "&a's inventory."));
    }

    private void handleEnderSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Util.color("&cOnly in-game admins can inspect ender chests."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin endersee <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer not online."));
            return;
        }

        viewer.openInventory(target.getEnderChest());
        viewer.sendMessage(Util.color("&aInspecting &e" + target.getName() + "&a's ender chest."));
    }

    private void handleTeleportTo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin tp <player> [target]"));
            return;
        }

        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Util.color("&cOnly in-game admins can teleport themselves."));
                return;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Util.color("&cPlayer not online."));
                return;
            }

            player.teleport(target.getLocation());
            player.sendMessage(Util.color("&aTeleported to &e" + target.getName() + "&a."));
            return;
        }

        Player mover = Bukkit.getPlayer(args[1]);
        Player destination = Bukkit.getPlayer(args[2]);

        if (mover == null || destination == null) {
            sender.sendMessage(Util.color("&cBoth players must be online to teleport."));
            return;
        }

        mover.teleport(destination.getLocation());
        sender.sendMessage(
                Util.color("&aTeleported &e" + mover.getName() + " &ato &e" + destination.getName() + "&a."));
        mover.sendMessage(Util.color("&eYou were teleported to &a" + destination.getName() + "&e."));
    }

    private void handleTeleportHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly in-game admins can teleport players."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin tphere <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer not online."));
            return;
        }

        target.teleport(player.getLocation());
        player.sendMessage(Util.color("&aTeleported &e" + target.getName() + " &ato you."));
        target.sendMessage(Util.color("&eYou were teleported to &a" + player.getName()));
    }

    private void handleHeal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin heal <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer not online."));
            return;
        }

        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        double amount = maxHealth != null ? maxHealth.getValue() : target.getHealth();
        target.setHealth(amount);
        target.setFireTicks(0);
        target.sendMessage(Util.color("&aYou have been healed by an admin."));
        sender.sendMessage(Util.color("&aHealed &e" + target.getName() + "&a."));
    }

    private void handleFeed(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin feed <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Util.color("&cPlayer not online."));
            return;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(Util.color("&aYour hunger has been restored by an admin."));
        sender.sendMessage(Util.color("&aFed &e" + target.getName() + "&a."));
    }

    private void handleLog(CommandSender sender, String[] args) {
        String actorFilter = null;
        int limit = 10;

        if (args.length >= 2) {
            actorFilter = args[1].equalsIgnoreCase("console") ? "Console" : args[1];
        }

        if (args.length >= 3) {
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Util.color("&cInvalid limit. Using default of 10."));
            }
        }

        List<String> entries = AdminLogManager.getRecentLogs(limit, actorFilter);
        if (entries.isEmpty()) {
            sender.sendMessage(Util.color("&eNo matching admin log entries found."));
            return;
        }

        String title = actorFilter == null ? "Recent Admin Activity" : "Admin Activity: " + actorFilter;
        MessageStyler.sendPanel(sender, title, entries.toArray(new String[0]));
    }

    private void sendAdminHelp(CommandSender sender, String label) {
        MessageStyler.sendPanel(sender, "Hardcore Admin Help",
                "&6Core",
                " &e/" + label + " list &7- show admins",
                " &e/" + label + " status <player> &7- flags",
                " &e/" + label + " log [admin] [limit] &7- recent actions",
                " &e/" + label + " help &7- this panel",
                "&6Moderation",
                " &e/" + label + " ban <player> [dur] [reason] &7- temp/perma ban",
                " &e/" + label + " unban <player> &7- lift ban",
                " &e/" + label + " mute <player> [dur] [reason] &7- temp/perma mute",
                " &e/" + label + " unmute <player> &7- lift mute",
                " &e/" + label + " warn <player> [reason] &7- send warning",
                " &e/" + label + " freeze <player> [reason] &7- lock in place",
                " &e/" + label + " unfreeze <player> &7- release",
                " &e/" + label + " kick <player> [reason] &7- kick now",
                " &e/" + label + " clearchat [reason] &7- wipe chat",
                "&6Insight",
                " &e/" + label + " info <player> &7- UUID/IP/pos/flags",
                " &e/" + label + " invsee <player> &7- view inventory",
                " &e/" + label + " endersee <player> &7- view ender chest",
                "&6Support",
                " &e/" + label + " tp <player> [target] &7- to player or player->player",
                " &e/" + label + " tphere <player> &7- pull to you",
                " &e/" + label + " heal <player> &7- restore health",
                " &e/" + label + " feed <player> &7- restore hunger");
    }

    private Duration parseDuration(String raw) {
        try {
            char unit = raw.toLowerCase(Locale.ROOT).charAt(raw.length() - 1);
            long value = Long.parseLong(raw.substring(0, raw.length() - 1));
            return switch (unit) {
                case 's' -> Duration.ofSeconds(value);
                case 'm' -> Duration.ofMinutes(value);
                case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value);
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean bootstrapAllowed = !AdminManager.hasAdmins()
                && (sender.hasPermission("hardcore.admin") || sender.isOp() || sender instanceof ConsoleCommandSender);
        boolean isOp = sender.isOp() || sender instanceof ConsoleCommandSender;
        if (!bootstrapAllowed && !AdminManager.isAdmin(sender) && !isOp) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartingWith(BASE_SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            switch (sub) {
                case "add":
                case "kick":
                case "mute":
                case "ban":
                case "warn":
                case "freeze":
                case "status":
                case "info":
                case "invsee":
                case "endersee":
                case "tp":
                case "tphere":
                case "heal":
                case "feed":
                    return filterStartingWith(onlinePlayerNames(), args[1]);
                case "remove":
                    return filterStartingWith(AdminManager.getAdminNames(), args[1]);
                case "unmute":
                    return filterStartingWith(MuteManager.getMutedNames(), args[1]);
                case "unban":
                    return filterStartingWith(BanManager.getBannedNames(), args[1]);
                case "unfreeze":
                    return filterStartingWith(FreezeManager.getFrozenNames(), args[1]);
                case "log": {
                    List<String> suggestions = new ArrayList<>(AdminManager.getAdminNames());
                    suggestions.add("Console");
                    return filterStartingWith(suggestions, args[1]);
                }
                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 3 && (sub.equals("mute") || sub.equals("ban"))) {
            return filterStartingWith(suggestDurations(), args[2]);
        }

        if (args.length == 3 && sub.equals("tp")) {
            return filterStartingWith(onlinePlayerNames(), args[2]);
        }

        if (args.length == 3 && sub.equals("log")) {
            return filterStartingWith(Arrays.asList("5", "10", "20", "50"), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filterStartingWith(List<String> source, String token) {
        List<String> result = new ArrayList<>();
        for (String option : source) {
            if (option.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) {
                result.add(option);
            }
        }
        return result;
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> suggestDurations() {
        return Arrays.asList("10m", "30m", "1h", "12h", "1d", "7d", "30d");
    }

    private String formatDurationText(Duration duration) {
        if (duration == null) {
            return "permanently";
        }

        return "for " + Util.formatDuration(duration.toMillis());
    }

    private OfflinePlayer resolveOfflinePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer cached = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (cached != null) {
            return cached;
        }

        UUID offlineUuid = UUID
                .nameUUIDFromBytes(("OfflinePlayer:" + name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
        return Bukkit.getOfflinePlayer(offlineUuid);
    }
}
