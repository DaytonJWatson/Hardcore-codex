package com.daytonjwatson.hardcore.commands;

import com.daytonjwatson.hardcore.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats <player>");
            return true;
        }

        UUID targetUuid;
        String targetName;

        if (args.length == 0) {
            Player player = (Player) sender;
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            String nameArg = args[0];

            Player online = Bukkit.getPlayerExact(nameArg);
            if (online != null) {
                targetUuid = online.getUniqueId();
                targetName = online.getName();
            } else {
                OfflinePlayer match = null;

                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().equalsIgnoreCase(nameArg)) {
                        match = op;
                        break;
                    }
                }

                if (match == null) {
                    sender.sendMessage(ChatColor.RED + "No data found for: " + nameArg);
                    return true;
                }

                targetUuid = match.getUniqueId();
                targetName = (match.getName() != null ? match.getName() : nameArg);
            }
        }

        StatsManager stats = StatsManager.get();

        int kills = stats.getKills(targetUuid);
        long lastDeath = stats.getLastDeath(targetUuid);
        String lastDeathCause = stats.getLastDeathCause(targetUuid);
        boolean isAlive = (lastDeath == 0L);
        long lifeMillis = stats.getLifeLengthMillis(targetUuid, isAlive);

        String lifeFormatted = formatDuration(lifeMillis);

        boolean isBandit = stats.isBandit(targetUuid);
        boolean isHero   = stats.isHero(targetUuid);

        int banditKills        = stats.getBanditKills(targetUuid);          // unfair kills (made you a bandit)
        int banditHunterKills  = stats.getBanditHunterKills(targetUuid);    // bandits slain while you were a bandit (redemption)
        int heroBanditKills    = stats.getHeroBanditKills(targetUuid);      // bandits slain while NOT a bandit (hero progress)

        String bar = ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------------";

        String statusText = isAlive ? ChatColor.GREEN + "ALIVE" : ChatColor.RED + "DEAD";
        String banditText = isBandit ? ChatColor.DARK_RED + "YES" : ChatColor.GREEN + "NO";
        String heroText   = isHero ? ChatColor.GREEN + "YES" : ChatColor.DARK_RED + "NO";
        String deathCauseText = (!isAlive && lastDeathCause != null && !lastDeathCause.isEmpty())
                ? lastDeathCause
                : "Unknown";

        // Same number of lines as previous version, just cleaner styling
        sender.sendMessage(bar);
        sender.sendMessage(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ HARDCORE STATS ☠ " +
                ChatColor.DARK_GRAY + "» " +
                ChatColor.GOLD + targetName
        );
        sender.sendMessage(
                ChatColor.DARK_GRAY + "Status: " +
                ChatColor.DARK_GRAY + "[" + statusText + ChatColor.DARK_GRAY + "]"
        );
        if (!isAlive) {
            sender.sendMessage(
                    ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Last Death" + ChatColor.DARK_GRAY + ": " +
                    ChatColor.WHITE + deathCauseText
            );
        }
        sender.sendMessage(
                ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Life" + ChatColor.DARK_GRAY + ": " +
                ChatColor.WHITE + lifeFormatted +
                ChatColor.DARK_GRAY + "  |  " +
                ChatColor.GOLD + "Kills" + ChatColor.DARK_GRAY + ": " +
                ChatColor.WHITE + kills
        );
        sender.sendMessage(
                ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Bandit" + ChatColor.DARK_GRAY + ": " +
                banditText +
                ChatColor.DARK_GRAY + "  |  " +
                ChatColor.GOLD + "Hero" + ChatColor.DARK_GRAY + ": " +
                heroText
        );
        sender.sendMessage(
                ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Unfair Kills" + ChatColor.DARK_GRAY + ": " +
                ChatColor.WHITE + banditKills +
                ChatColor.DARK_GRAY + "  |  " +
                ChatColor.GOLD + "Redeem Kills" + ChatColor.DARK_GRAY + ": " +
                ChatColor.WHITE + banditHunterKills
        );
        sender.sendMessage(
                ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Hero kills" + ChatColor.DARK_GRAY + ": " +
                ChatColor.WHITE + heroBanditKills
        );
        sender.sendMessage(bar);

        return true;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0m";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (days == 0 && hours == 0 && minutes == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> results = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name == null) continue;
                if (name.toLowerCase().startsWith(partial)) {
                    results.add(name);
                }
            }

            // Only look at offline players if the user has typed at least 3 chars
            if (partial.length() >= 3) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    String name = op.getName();
                    if (name == null) continue;
                    String lower = name.toLowerCase();

                    if (lower.startsWith(partial) && !results.contains(name)) {
                        results.add(name);
                    }
                }
            }
        }

        return results;
    }
}
