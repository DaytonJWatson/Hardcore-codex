package com.daytonjwatson.hardcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.Util;

public class PlayerChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        StatsManager stats = StatsManager.get();

        if (stats == null || !ConfigValues.chatEnabled()) {
            return;
        }

        boolean isBandit = stats.isBandit(player.getUniqueId());
        boolean isHero = stats.isHero(player.getUniqueId());

        String prefix = "";

        // Bandit takes priority if somehow both are true
        if (isBandit) {
            prefix = ConfigValues.chatBanditPrefix();
        }
        else if (isHero) {
            prefix = ConfigValues.chatHeroPrefix();
        }

        String format = ConfigValues.chatFormat()
                .replace("%prefix%", prefix)
                .replace("%name_color%", ConfigValues.chatNameColor());

        event.setFormat(Util.color(format));
    }
}
