package com.daytonjwatson.hardcore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.managers.AdminManager;
import com.daytonjwatson.hardcore.managers.BanManager;
import com.daytonjwatson.hardcore.managers.DeathBanManager;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.DeathMessageHelper;
import com.daytonjwatson.hardcore.utils.TabUtil;
import com.daytonjwatson.hardcore.utils.Util;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (AdminManager.isAdmin(victim.getUniqueId())) {
            return;
        }

        StatsManager stats = StatsManager.get();
        stats.handleDeath(victim, killer);

        // No killer = environmental / mob / suicide → still do sound/tab, but no PvP announcements
        if (killer != null) {
            event.setDeathMessage(null);

            String killMessage = DeathMessageHelper.buildDeathMessage(victim, killer, stats);
            if (killMessage != null) {
                killer.getServer().broadcastMessage(killMessage);
            }
        }

        // Update tab prefixes / status (Bandit / Hero etc.)
        TabUtil.updateTabForAll();

        // Global death sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), ConfigValues.deathSound(), 1.0f, 1.0f);
        }

        String killerName = killer != null ? killer.getName() : "Environment";
        String reason = killer != null ? "Killed by " + killerName : "Died to the environment";

        BanManager.ban(victim, reason, null, "Deathban");
        DeathBanManager.recordDeathBan(victim, reason, killerName);

        victim.kickPlayer(Util.color("&cYou died and have been permanently banned.\n&7Reason: " + reason));
    }
}
