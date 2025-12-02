package com.daytonjwatson.hardcore.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.daytonjwatson.hardcore.managers.FreezeManager;
import com.daytonjwatson.hardcore.utils.Util;

public class FreezeListener implements Listener {

    private final Map<UUID, Long> lastNotify = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!FreezeManager.isFrozen(uuid)) {
            return;
        }

        if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
            event.getPlayer().teleport(event.getFrom());
        }

        long now = System.currentTimeMillis();
        long last = lastNotify.getOrDefault(uuid, 0L);
        if (now - last > 3000) {
            lastNotify.put(uuid, now);
            event.getPlayer().sendMessage(Util.color("&cYou are frozen by an admin. Please wait."));
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!FreezeManager.isFrozen(uuid)) {
            return;
        }

        String lower = event.getMessage().toLowerCase();
        if (lower.startsWith("/admin")) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(Util.color("&cYou cannot use commands while frozen."));
    }
}
