package com.daytonjwatson.hardcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageStyler {

    private static final String BAR = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "------------------------------";
    private static final String BULLET = ChatColor.DARK_GRAY + "⟡ " + ChatColor.GRAY;
    private static final String PREFIX = ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + ChatColor.BOLD;

    private MessageStyler() {
    }

    public static void sendPanel(CommandSender target, String title, String... lines) {
        target.sendMessage(BAR);
        target.sendMessage(PREFIX + Util.color(title));
        for (String line : lines) {
            target.sendMessage(BULLET + Util.color(line));
        }
        target.sendMessage(BAR);
    }

    public static void broadcastPanel(String title, String... lines) {
        Bukkit.broadcastMessage(BAR);
        Bukkit.broadcastMessage(PREFIX + Util.color(title));
        for (String line : lines) {
            Bukkit.broadcastMessage(BULLET + Util.color(line));
        }
        Bukkit.broadcastMessage(BAR);
    }

    public static String bulletLine(String label, ChatColor labelColor, String text) {
        return BULLET + labelColor + label + ChatColor.DARK_GRAY + ChatColor.BOLD + ChatColor.RESET + ChatColor.GRAY + " " + text;
    }

    public static String bulletText(String text) {
        return BULLET + text;
    }

    public static String compactHighlight(String label, ChatColor labelColor, String text) {
        return ChatColor.DARK_GRAY + labelColor.toString() + ChatColor.BOLD + label + ChatColor.DARK_GRAY + ChatColor.BOLD + ChatColor.RESET + ChatColor.GRAY + " " + text;
    }

    public static String simplePrefix(String title, String text) {
        return ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + ChatColor.BOLD + title + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + text;
    }

    public static String bar() {
        return BAR;
    }
}
