package com.daytonjwatson.hardcore.listeners;

import java.net.InetSocketAddress;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.PlayerIpManager;
import com.daytonjwatson.hardcore.utils.Util;

public class PlayerLoginListener implements Listener {

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        PlayerIpManager.recordLogin(event.getPlayer(), event.getAddress());

        if (!BanManager.isBanned(event.getPlayer().getUniqueId())) {
            return;
        }

        long remaining = BanManager.getRemainingMillis(event.getPlayer().getUniqueId());
        String durationText = remaining == -1L ? "permanently" : "for " + Util.formatDuration(remaining);
        String reason = BanManager.getReason(event.getPlayer().getUniqueId());

        event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                Util.color("&cYou are banned. " + durationText + ". Reason: &7" + reason));
    }
}
