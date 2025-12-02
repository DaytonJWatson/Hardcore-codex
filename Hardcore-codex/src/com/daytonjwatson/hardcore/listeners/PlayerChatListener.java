package com.daytonjwatson.hardcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.daytonjwatson.hardcore.managers.StatsManager;

public class PlayerChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        StatsManager stats = StatsManager.get();

        boolean isBandit = stats.isBandit(player.getUniqueId());
        boolean isHero = stats.isHero(player.getUniqueId());

        String nameColor = ChatColor.GRAY.toString();
        String prefix = "";

        // Bandit takes priority if somehow both are true
        if (isBandit) {
            prefix = ChatColor.DARK_RED + "" + ChatColor.BOLD + "[B] " + ChatColor.RESET;
        } 
        else if (isHero) {
            prefix = ChatColor.GOLD + "" + ChatColor.BOLD + "[H] " + ChatColor.RESET;
        }

        String format = prefix 
                + ChatColor.GRAY + " <" 
                + nameColor + "%s" 
                + ChatColor.RESET + ChatColor.GRAY + "> %s";

        event.setFormat(format);
    }
}
