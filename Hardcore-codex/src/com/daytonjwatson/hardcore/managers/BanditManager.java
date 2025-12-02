package com.daytonjwatson.hardcore.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.utils.GearPowerUtil;
import com.daytonjwatson.hardcore.utils.GearPowerUtil.CombatSnapshot;

public class BanditManager {

    private static final int MIN_KILLER_GEAR_POWER = 6;
    private static final int MIN_BASE_GEAR_GAP = 4;
    private static final double MIN_EFFECTIVE_TOTAL_GAP = 3.0;

    /**
     * @return true  → killer JUST became a bandit this kill
     * @return false → killer did not become a bandit
     */
    public static boolean handlePotentialBanditKill(Player victim, Player killer) {
        if (victim == null || killer == null) {
            return false;
        }

        CombatSnapshot killerSnap = GearPowerUtil.getCombatSnapshot(killer);
        CombatSnapshot victimSnap = GearPowerUtil.getCombatSnapshot(victim);

        int killerGearPower = killerSnap.gearPower;
        int victimGearPower = victimSnap.gearPower;
        double killerTotal = killerSnap.totalPower;
        double victimTotal = victimSnap.totalPower;

        double baseGearDiff = killerGearPower - victimGearPower;
        double effectiveDiff = killerTotal - victimTotal;

        // Basic requirements to count as unfair
        if (killerGearPower < MIN_KILLER_GEAR_POWER) return false;
        if (baseGearDiff < MIN_BASE_GEAR_GAP) return false;
        if (effectiveDiff < MIN_EFFECTIVE_TOTAL_GAP) return false;

        // Unfair kill confirmed
        StatsManager stats = StatsManager.get();
        stats.handleUnfairKill(killer.getUniqueId());

        int banditKills = stats.getBanditKills(killer.getUniqueId());
        boolean nowBandit = stats.isBandit(killer.getUniqueId());

        // Player gained an unfair kill but is NOT bandit yet
        if (!nowBandit) {
            killer.sendMessage(ChatColor.DARK_RED +
                    "You are gaining a reputation as a bandit (" + banditKills + "/3).");
            return false;
        }

        // IMPORTANT:
        // Only return TRUE when the killer *just became* a bandit.
        return banditKills == 3;
    }

    private BanditManager() {}
}
