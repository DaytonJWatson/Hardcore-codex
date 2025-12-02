package com.daytonjwatson.hardcore.utils;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.managers.StatsManager;

public class TabUtil {

    public static void updateTabForAll() {
        StatsManager stats = StatsManager.get();
        if (stats == null) {
            return;
        }

        if (!ConfigValues.tablistEnabled()) {
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

        if (!ConfigValues.tablistEnabled()) {
            return;
        }

        updateTabForPlayer(player, stats);
    }

    private static void updateTabForPlayer(Player player, StatsManager stats) {
        int uniquePlayers = stats.getUniquePlayerCount();
        int totalDeaths = stats.getTotalDeaths();
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        UUID uuid = player.getUniqueId();

        int kills = stats.getKills(uuid);
        int deaths = stats.getDeaths(uuid);

        boolean isBandit = stats.isBandit(uuid);
        boolean isHero = stats.isHero(uuid);

        boolean isAlive = (stats.getLastDeath(uuid) == 0L);
        long lifeMillis = stats.getLifeLengthMillis(uuid, isAlive);

        String lifeFormatted = formatDuration(lifeMillis);
        String statusText = isAlive ? ChatColor.GREEN + "ALIVE" : ChatColor.RED + "DEAD";
        String roleText;

        if (isBandit) {
            roleText = ChatColor.DARK_RED + "Bandit";
        } else if (isHero) {
            roleText = ChatColor.GOLD + "Hero";
        } else if (stats.hasEverBeenBandit(uuid)) {
            roleText = ChatColor.GRAY + "Redeemed";
        } else {
            roleText = ChatColor.GREEN + "Adventurer";
        }

        String header = buildHeader(uniquePlayers, totalDeaths, online, max);

        String footer = buildFooter(statusText, roleText, lifeFormatted, kills, deaths);

        player.setPlayerListHeaderFooter(header, footer);
    }

    private static String buildHeader(int uniquePlayers, int totalDeaths, int online, int max) {
        var headerSection = ConfigValues.tabHeader();
        if (headerSection == null) {
            return "";
        }

        String title = headerSection.getString("title", ChatColor.DARK_RED + "" + ChatColor.BOLD + "☠ HARDCORE ☠");
        boolean showOnline = headerSection.getBoolean("show-online", true);
        boolean showUnique = headerSection.getBoolean("show-unique", true);
        boolean showTotalDeaths = headerSection.getBoolean("show-total-deaths", true);

        String onlineLine = ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Online" + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + online + ChatColor.DARK_GRAY + " / " + ChatColor.WHITE + max;
        String uniqueLine = ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Unique" + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + uniquePlayers + ChatColor.DARK_GRAY + "  |  " + ChatColor.GOLD + "Total deaths" + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + totalDeaths;

        StringBuilder sb = new StringBuilder();
        sb.append(MessageStyler.bar()).append("\n").append(Util.color(title));
        if (showOnline) {
            sb.append("\n").append(onlineLine);
        }
        if (showUnique || showTotalDeaths) {
            sb.append("\n").append(uniqueLine);
        }

        return sb.toString();
    }

    private static String buildFooter(String statusText, String roleText, String lifeFormatted, int kills, int deaths) {
        var footerSection = ConfigValues.tabFooter();
        if (footerSection == null) {
            return "";
        }

        boolean showStatus = footerSection.getBoolean("show-status", true);
        boolean showRole = footerSection.getBoolean("show-role", true);
        boolean showLife = footerSection.getBoolean("show-life", true);
        boolean showKd = footerSection.getBoolean("show-kd", true);
        String commandsLine = footerSection.getString("commands-line", ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Commands" + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY + "/guide" + ChatColor.DARK_GRAY + " · " + ChatColor.GRAY + "/rules" + ChatColor.DARK_GRAY + " · " + ChatColor.GRAY + "/stats" + ChatColor.DARK_GRAY + " · " + ChatColor.GRAY + "/bandittracker");

        String statusLine = ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Status" + ChatColor.DARK_GRAY + ": " + statusText + ChatColor.DARK_GRAY + "  |  " + ChatColor.GOLD + "Role" + ChatColor.DARK_GRAY + ": " + roleText;
        String lifeLine = ChatColor.DARK_GRAY + "⟡ " + ChatColor.GOLD + "Life" + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + lifeFormatted + ChatColor.DARK_GRAY + "  |  " + ChatColor.GOLD + "K/D" + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + kills + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + deaths;

        StringBuilder sb = new StringBuilder();
        if (showStatus || showRole) {
            sb.append(statusLine).append("\n");
        }
        if (showLife || showKd) {
            sb.append(lifeLine).append("\n");
        }

        sb.append(Util.color(commandsLine)).append("\n").append(MessageStyler.bar());

        return sb.toString();
    }

    private static String formatDuration(long millis) {
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
}
