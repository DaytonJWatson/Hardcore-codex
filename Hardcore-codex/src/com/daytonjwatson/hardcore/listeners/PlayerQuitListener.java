package com.daytonjwatson.hardcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.TabUtil;
import com.daytonjwatson.hardcore.utils.Util;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        StatsManager.get().handleQuit(player);

        event.setQuitMessage(Util.color("&7" + player.getName() + " has &cleft"));

        TabUtil.updateTabForAll();
    }
}
