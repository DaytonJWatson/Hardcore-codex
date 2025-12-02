package com.daytonjwatson.hardcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.managers.StatsManager;

public class TabUtil {

    public static void updateTabForAll() {
        StatsManager stats = StatsManager.get();
        if (stats == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabForPlayer(player, stats);
        }
    }

    public static void updateTabForPlayer(Player player) {
        StatsManager stats = StatsManager.get();
        if (stats == null) {
            return;
        }

        updateTabForPlayer(player, stats);
    }

    private static void updateTabForPlayer(Player player, StatsManager stats) {
        int uniquePlayers = stats.getUniquePlayerCount();
        int totalDeaths = stats.getTotalDeaths();
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        String header =
                "";

        String footer =
                ChatColor.GOLD + "Online: " + ChatColor.GRAY + online + ChatColor.DARK_GRAY + " / " + ChatColor.GRAY + max + "\n" +
                ChatColor.GOLD + "Unique players: " + ChatColor.GRAY + uniquePlayers + "\n" +
                ChatColor.GOLD + "Total deaths: " + ChatColor.GRAY + totalDeaths;

        player.setPlayerListHeaderFooter(header, footer);
    }
}