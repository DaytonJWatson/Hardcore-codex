package com.daytonjwatson.hardcore.commands;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.MuteManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.Util;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> BASE_SUBCOMMANDS = Arrays.asList(
            "help", "add", "remove", "list", "ban", "unban", "kick", "mute", "unmute", "status");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean bootstrapAllowed = !AdminManager.hasAdmins() && sender.hasPermission("hardcore.admin");
        if (!bootstrapAllowed && !AdminManager.isAdmin(sender)) {
            // Keep the command hidden from non-admins while hinting bootstrap path to permitted staff
            if (sender.hasPermission("hardcore.admin")) {
                sender.sendMessage(Util.color("&eAdd yourself first with &7/console> admin add <name>&e."));
            } else {
                sender.sendMessage("Unknown command. Type \"/help\" for help.");
            }
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "ban":
                handleBan(sender, args);
                break;
            case "unban":
                handleUnban(sender, args);
                break;
            case "kick":
                handleKick(sender, args);
                break;
            case "mute":
                handleMute(sender, args);
                break;
            case "unmute":
                handleUnmute(sender, args);
                break;
            case "status":
                handleStatus(sender, args);
                break;
            default:
                sendAdminHelp(sender, label);
                break;
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin add <player>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        boolean added = AdminManager.addAdmin(target);
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
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
            sender.sendMessage(Util.color("&cUsage: /admin ban <player> [reason]"));
            return;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "Banned by Hardcore admin.";

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, null, sender.getName());

        if (target.isOnline()) {
            ((Player) target).kickPlayer(Util.color("&cYou have been banned. Reason: &7" + reason));
        }

        Bukkit.broadcast(Util.color("&4&l[ADMIN]&c " + targetName + " was banned: &7" + reason), "hardcore.admin");
        sender.sendMessage(Util.color("&aBanned &e" + targetName + "&a."));
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin unban <player>"));
            return;
        }

        String targetName = args[1];
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        sender.sendMessage(Util.color("&aUnbanned &e" + targetName + "&a."));
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        boolean success = MuteManager.unmute(target.getUniqueId());
        if (success) {
            sender.sendMessage(Util.color("&aUnmuted &e" + targetName + "&a."));
        } else {
            sender.sendMessage(Util.color("&e" + targetName + " &cis not muted."));
        }
    }

    private void handleStatus(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.color("&cUsage: /admin status <player>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();
        boolean isMuted = MuteManager.isMuted(uuid);
        boolean isAdmin = AdminManager.isAdmin(uuid);

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

        MessageStyler.sendPanel(sender, target.getName() + " status", lines.toArray(new String[0]));
    }

    private void sendAdminHelp(CommandSender sender, String label) {
        MessageStyler.sendPanel(sender, "Hardcore Admin Help",
                "&e/" + label + " add <player> &7- Make a player a Hardcore admin.",
                "&e/" + label + " remove <player> &7- Remove admin access.",
                "&e/" + label + " list &7- View configured admins.",
                "&e/" + label + " ban <player> [reason] &7- Permanently ban a player.",
                "&e/" + label + " unban <player> &7- Remove a ban.",
                "&e/" + label + " kick <player> [reason] &7- Kick a player from the server.",
                "&e/" + label + " mute <player> [duration] [reason] &7- Mute chat for a player.",
                "&e/" + label + " unmute <player> &7- Lift a mute.",
                "&e/" + label + " status <player> &7- See admin/mute status.");
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
        if (!AdminManager.isAdmin(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartingWith(BASE_SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && Arrays.asList("add", "remove", "ban", "unban", "kick", "mute", "unmute", "status")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterStartingWith(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("mute")) {
            return filterStartingWith(Arrays.asList("10m", "1h", "1d"), args[2]);
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
}
