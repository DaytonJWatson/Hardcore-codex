package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.DeathMessageHelper;
import com.daytonjwatson.hardcore.utils.TabUtil;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        StatsManager stats = StatsManager.get();
        stats.handleDeath(victim, killer);

        // No killer = environmental / mob / suicide → still do sound/tab, but no PvP announcements
        if (killer != null) {
            event.setDeathMessage(null);

            DeathMessageHelper.broadcastDeathMessage(victim, killer, stats);
        }

        // Update tab prefixes / status (Bandit / Hero etc.)
        TabUtil.updateTabForAll();

        // Global death sound
        Sound sound = Sound.ENTITY_WITHER_DEATH;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}
