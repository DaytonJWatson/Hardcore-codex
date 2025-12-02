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
        int deaths = stats.getDeaths(targetUuid);
        long lastDeath = stats.getLastDeath(targetUuid);
        boolean isAlive = (lastDeath == 0L);
        long lifeMillis = stats.getLifeLengthMillis(targetUuid, isAlive);

        String lifeFormatted = formatDuration(lifeMillis);

        double kd = (deaths == 0 ? kills : (double) kills / deaths);

        boolean isBandit = stats.isBandit(targetUuid);
        boolean isHero   = stats.isHero(targetUuid);

        int banditKills        = stats.getBanditKills(targetUuid);          // unfair kills (made you a bandit)
        int banditHunterKills  = stats.getBanditHunterKills(targetUuid);    // bandits slain while you were a bandit (redemption)
        int heroBanditKills    = stats.getHeroBanditKills(targetUuid);      // bandits slain while NOT a bandit (hero progress)

        sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "========== HARDCORE STATS ==========");
        sender.sendMessage(ChatColor.GOLD + "Player: " + ChatColor.GRAY + targetName);
        sender.sendMessage(ChatColor.GOLD + "Status: " +
                (isAlive ? ChatColor.GREEN + "Alive" : ChatColor.RED + "Dead"));
        sender.sendMessage(ChatColor.GOLD + "Life length: " + ChatColor.GRAY + lifeFormatted);
        sender.sendMessage(ChatColor.GOLD + "Kills: " + ChatColor.GRAY + kills);
        sender.sendMessage(ChatColor.GOLD + "Deaths: " + ChatColor.GRAY + deaths);
        sender.sendMessage(ChatColor.GOLD + "K/D: " + ChatColor.GRAY + String.format("%.2f", kd));

        sender.sendMessage(ChatColor.GOLD + "Bandit: " +
                (isBandit ? ChatColor.DARK_RED + "Yes" : ChatColor.GREEN + "No"));
        sender.sendMessage(ChatColor.GOLD + "Hero: " +
                (isHero ? ChatColor.GREEN + "Yes" : ChatColor.DARK_RED + "No"));

        sender.sendMessage(ChatColor.GOLD + "Bandit kills (unfair kills): " + ChatColor.GRAY + banditKills);
        sender.sendMessage(ChatColor.GOLD + "Bandits slain as Bandit (redemption): " + ChatColor.GRAY + banditHunterKills);
        sender.sendMessage(ChatColor.GOLD + "Bandits slain as Non-Bandit (Hero progress): " + ChatColor.GRAY + heroBanditKills);

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
