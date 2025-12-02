package com.daytonjwatson.hardcore.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.managers.BanditManager;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.GearPowerUtil.CombatSnapshot;
import com.daytonjwatson.hardcore.utils.MessageStyler;

public final class DeathMessageHelper {

    // Mirror BanditManager thresholds so "UNFAIR" matches bandit logic
    private static final int MIN_KILLER_GEAR_POWER = 6;
    private static final int MIN_BASE_GEAR_GAP    = 4;
    private static final double MIN_EFFECTIVE_TOTAL_GAP = 3.0;

    private static final String[] FAIR_MESSAGES = {
            "%KILLER% outplayed %VICTIM%",
            "%KILLER% defeated %VICTIM% in a fair fight",
            "%KILLER% bested %VICTIM%",
            "%KILLER% won a sweaty duel against %VICTIM%",
            "%KILLER% sent %VICTIM% back to the main menu",
            "%KILLER% proved to be the better fighter than %VICTIM%",
            "%KILLER% traded hits with %VICTIM% and came out on top",
            "%KILLER% narrowly clutched up against %VICTIM%",
            "%KILLER% showed solid mechanics against %VICTIM%",
            "%KILLER% outmaneuvered %VICTIM% in close combat"
    };

    private static final String[] UNFAIR_MESSAGES = {
            "%KILLER% cowardly murdered %VICTIM%",
            "%KILLER% steamrolled %VICTIM% with overwhelming gear",
            "%KILLER% abused their gear advantage against %VICTIM%",
            "%KILLER% ambushed a helpless %VICTIM%",
            "%KILLER% turned %VICTIM% into target practice",
            "%KILLER% deleted %VICTIM% from the world",
            "%KILLER% treated %VICTIM% like a walking loot box",
            "%KILLER% farmed free stats off %VICTIM%",
            "%KILLER% crushed any hope %VICTIM% had of surviving",
            "%KILLER% reminded %VICTIM% that life isn’t fair"
    };

    private static final String[] HERO_VS_BANDIT_MESSAGES = {
            "%KILLER% hunted down bandit %VICTIM%",
            "%KILLER% brought justice to bandit %VICTIM%",
            "%KILLER% purged the notorious bandit %VICTIM%",
            "%KILLER% ended the crime spree of bandit %VICTIM%",
            "%KILLER% proved that heroes win over bandit %VICTIM%",
            "%KILLER% put a bounty-style finish on bandit %VICTIM%",
            "%KILLER% finally caught up to bandit %VICTIM%",
            "%KILLER% sent bandit %VICTIM% to their final respawn",
            "%KILLER% made an example out of bandit %VICTIM%",
            "%KILLER% delivered righteous punishment to bandit %VICTIM%"
    };

    private static final String[] REGULAR_VS_BANDIT_MESSAGES = {
            "%KILLER% took down bandit %VICTIM%",
            "%KILLER% got revenge on bandit %VICTIM%",
            "%KILLER% clapped bandit %VICTIM%",
            "%KILLER% refused to be another victim of bandit %VICTIM%",
            "%KILLER% shut down the plans of bandit %VICTIM%",
            "%KILLER% surprised bandit %VICTIM% with a clean kill",
            "%KILLER% turned the tables on bandit %VICTIM%",
            "%KILLER% reminded bandit %VICTIM% that prey can bite back",
            "%KILLER% outsmarted bandit %VICTIM%",
            "%KILLER% sent bandit %VICTIM% back to the lobby"
    };

    private static final String[] BANDIT_VS_BANDIT_MESSAGES = {
            "%KILLER% turned on fellow bandit %VICTIM%",
            "%KILLER% betrayed bandit %VICTIM%",
            "%KILLER% removed rival bandit %VICTIM% from the game",
            "%KILLER% proved to be the stronger bandit over %VICTIM%",
            "%KILLER% decided there was only room for one bandit, and it wasn’t %VICTIM%",
            "%KILLER% settled a bandit dispute by killing %VICTIM%",
            "%KILLER% out-cheesed bandit %VICTIM%",
            "%KILLER% ended the partnership with bandit %VICTIM% permanently",
            "%KILLER% showed no loyalty to bandit %VICTIM%",
            "%KILLER% used bandit %VICTIM% as a stepping stone"
    };

    private DeathMessageHelper() {
    }

    public static String buildDeathMessage(Player victim, Player killer, StatsManager stats) {
        UUID victimId = victim.getUniqueId();
        UUID killerId = killer.getUniqueId();

        boolean victimWasBandit = stats.isBandit(victimId);
        boolean killerWasBandit = stats.isBandit(killerId);
        boolean killerWasHero = stats.isHero(killerId);

        if (victimWasBandit) {
            handleBanditVictim(killer, killerId, killerWasBandit, killerWasHero, stats);
        } else {
            handleRegularVictim(victim, killer, killerWasHero, stats);
        }

        CombatSnapshot killerSnap = GearPowerUtil.getCombatSnapshot(killer);
        CombatSnapshot victimSnap = GearPowerUtil.getCombatSnapshot(victim);

        boolean killerIsBanditNow = stats.isBandit(killerId);
        boolean killerIsHeroNow   = stats.isHero(killerId);

        boolean victimIsBanditNow = stats.isBandit(victimId);
        boolean victimIsHeroNow   = stats.isHero(victimId);

        String killerPrefix =
                killerIsBanditNow ? ChatColor.DARK_RED + "" + ChatColor.BOLD + "[B] " + ChatColor.RED :
                killerIsHeroNow   ? ChatColor.GOLD     + "" + ChatColor.BOLD + "[H] " + ChatColor.RED :
                                    "";

        String victimPrefix =
                victimIsBanditNow ? ChatColor.DARK_RED + "" + ChatColor.BOLD + "[B] " + ChatColor.RESET :
                victimIsHeroNow   ? ChatColor.GOLD     + "" + ChatColor.BOLD + "[H] " + ChatColor.RESET :
                                    "";

        double baseGearDiff  = killerSnap.gearPower - victimSnap.gearPower;
        double effectiveDiff = killerSnap.totalPower - victimSnap.totalPower;

        boolean unfair =
                !victimWasBandit &&
                killerSnap.gearPower >= MIN_KILLER_GEAR_POWER &&
                baseGearDiff         >= MIN_BASE_GEAR_GAP &&
                effectiveDiff        >= MIN_EFFECTIVE_TOTAL_GAP;

        String killerDisplay = ChatColor.RED   + killerPrefix + killer.getName() + ChatColor.GRAY;
        String victimDisplay = ChatColor.WHITE + victimPrefix + victim.getName() + ChatColor.GRAY;

        String template;
        if (victimWasBandit) {
            if (killerIsBanditNow) {
                template = pickRandom(BANDIT_VS_BANDIT_MESSAGES);
            } else if (killerIsHeroNow) {
                template = pickRandom(HERO_VS_BANDIT_MESSAGES);
            } else {
                template = pickRandom(REGULAR_VS_BANDIT_MESSAGES);
            }
        } else if (unfair) {
            template = pickRandom(UNFAIR_MESSAGES);
        } else {
            template = pickRandom(FAIR_MESSAGES);
        }

        return template
                .replace("%KILLER%", killerDisplay)
                .replace("%VICTIM%", victimDisplay)
                + ChatColor.GRAY + " ("
                + ChatColor.RED + "Gear " + killerSnap.gearPower
                + ChatColor.GRAY + " vs "
                + ChatColor.WHITE + "Gear " + victimSnap.gearPower
                + ChatColor.GRAY + ", "
                + ChatColor.RED + "Power " + (int) Math.round(killerSnap.totalPower)
                + ChatColor.GRAY + " vs "
                + ChatColor.WHITE + "Power " + (int) Math.round(victimSnap.totalPower)
                + ChatColor.GRAY + ")";
    }

    private static void handleBanditVictim(Player killer, UUID killerId, boolean killerWasBandit,
            boolean killerWasHero, StatsManager stats) {
        if (killerWasBandit) {
            boolean lostBandit = stats.handleBanditKill(killerId);
            int hunterKills = stats.getBanditHunterKills(killerId);
            int needed = Math.max(0, 3 - hunterKills);

            if (lostBandit) {
                MessageStyler.sendPanel(killer, "Bandit Redemption",
                        ChatColor.GREEN + "You have redeemed yourself by slaying 3 bandits.",
                        ChatColor.GRAY + "Your bandit status has been removed.");

                MessageStyler.broadcastPanel("Bandit Redeemed",
                        ChatColor.GREEN + killer.getName() + ChatColor.GRAY + " has slain 3 bandits and is no longer a " +
                                ChatColor.DARK_RED + "Bandit" + ChatColor.GRAY + ".");
            } else {
                MessageStyler.sendPanel(killer, "Bandit Redemption",
                        ChatColor.YELLOW + "Progress: " + ChatColor.GOLD + hunterKills + ChatColor.GRAY + "/3",
                        ChatColor.GRAY + "You need " + ChatColor.RED + needed + ChatColor.GRAY + " more bandit kills.");
            }
        } else {
            boolean becameHero = stats.handleHeroBanditKill(killerId);
            int heroKills = stats.getHeroBanditKills(killerId);
            int needed = Math.max(0, 3 - heroKills);

            if (becameHero) {
                MessageStyler.sendPanel(killer, "Hero Status Achieved",
                        ChatColor.GOLD + "You are now recognized as a HERO for slaying 3 bandits!");

                MessageStyler.broadcastPanel("Hero Ascended",
                        ChatColor.GOLD + killer.getName() + ChatColor.GRAY + " is now a " + ChatColor.GOLD + ChatColor.BOLD +
                                "HERO " + ChatColor.GRAY + "after defeating 3 bandits!");
            } else if (!killerWasHero && !stats.isHero(killerId)) {
                MessageStyler.sendPanel(killer, "Hero Progress",
                        ChatColor.YELLOW + "Bandits slain: " + ChatColor.GOLD + heroKills + ChatColor.GRAY + "/3",
                        ChatColor.GRAY + "You need " + ChatColor.GOLD + needed + ChatColor.GRAY +
                                " more bandit kills to become a Hero.");
            }
        }
    }

    private static void handleRegularVictim(Player victim, Player killer, boolean killerWasHero, StatsManager stats) {
        boolean becameBandit = BanditManager.handlePotentialBanditKill(victim, killer);

        if (!becameBandit) {
            return;
        }

        UUID killerId = killer.getUniqueId();
        boolean killerIsNowBandit = stats.isBandit(killerId);
        boolean killerIsNowHero   = stats.isHero(killerId);

        if (killerWasHero && killerIsNowBandit && !killerIsNowHero) {
            MessageStyler.sendPanel(killer, "Demoted to Bandit",
                    ChatColor.RED + "You have fallen from HERO to BANDIT for attacking weaker players.");

            MessageStyler.broadcastPanel("Hero Demoted",
                    ChatColor.GOLD + killer.getName() + ChatColor.GRAY + " has fallen from " +
                            ChatColor.GOLD + ChatColor.BOLD + "HERO " + ChatColor.GRAY + "to " +
                            ChatColor.DARK_RED + ChatColor.BOLD + "BANDIT " + ChatColor.GRAY +
                            "after repeatedly attacking weaker players.");
        } else {
            MessageStyler.sendPanel(killer, "Bandit Status",
                    ChatColor.RED + "You are now a BANDIT. Your violent reputation precedes you...");

            MessageStyler.broadcastPanel("New Bandit",
                    ChatColor.RED + killer.getName() + ChatColor.GRAY + " has become a " +
                            ChatColor.DARK_RED + ChatColor.BOLD + "BANDIT " + ChatColor.GRAY +
                            "after repeatedly preying on weaker players.");
        }
    }

    private static String pickRandom(String[] pool) {
        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }
}
