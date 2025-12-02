package com.daytonjwatson.hardcore.utils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.managers.BanditManager;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.GearPowerUtil.CombatSnapshot;

public final class DeathMessageHelper {

    private static final List<String> DEFAULT_FAIR_MESSAGES = List.of(
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
    );

    private static final List<String> DEFAULT_UNFAIR_MESSAGES = List.of(
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
    );

    private static final List<String> DEFAULT_HERO_VS_BANDIT_MESSAGES = List.of(
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
    );

    private static final List<String> DEFAULT_REGULAR_VS_BANDIT_MESSAGES = List.of(
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
    );

    private static final List<String> DEFAULT_BANDIT_VS_BANDIT_MESSAGES = List.of(
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
    );

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
                killerSnap.gearPower >= ConfigValues.minKillerGearPower() &&
                baseGearDiff         >= ConfigValues.minBaseGearGap() &&
                effectiveDiff        >= ConfigValues.minEffectiveTotalGap();

        String killerDisplay = ChatColor.RED   + killerPrefix + killer.getName() + ChatColor.GRAY;
        String victimDisplay = ChatColor.WHITE + victimPrefix + victim.getName() + ChatColor.GRAY;

        String template;
        if (victimWasBandit) {
            if (killerIsBanditNow) {
                template = pickRandom(ConfigValues.deathMessages("bandit-vs-bandit", DEFAULT_BANDIT_VS_BANDIT_MESSAGES));
            } else if (killerIsHeroNow) {
                template = pickRandom(ConfigValues.deathMessages("hero-vs-bandit", DEFAULT_HERO_VS_BANDIT_MESSAGES));
            } else {
                template = pickRandom(ConfigValues.deathMessages("regular-vs-bandit", DEFAULT_REGULAR_VS_BANDIT_MESSAGES));
            }
        } else if (unfair) {
            template = pickRandom(ConfigValues.deathMessages("unfair", DEFAULT_UNFAIR_MESSAGES));
        } else {
            template = pickRandom(ConfigValues.deathMessages("fair", DEFAULT_FAIR_MESSAGES));
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
            int redemptionNeeded = ConfigValues.redemptionKills();
            int needed = Math.max(0, redemptionNeeded - hunterKills);

            if (lostBandit) {
                killer.sendMessage(ChatColor.GREEN +
                        "You have redeemed yourself by slaying " + redemptionNeeded + " bandits. Your bandit status has been removed.");

                Bukkit.broadcastMessage(
                        ChatColor.DARK_GRAY + "[" +
                                ChatColor.GREEN + "" + ChatColor.BOLD + "REDEEMED" +
                                ChatColor.DARK_GRAY + "] " +
                                ChatColor.GREEN + killer.getName() +
                                ChatColor.GRAY + " has redeemed themselves by slaying " + redemptionNeeded + " bandits and is no longer a " +
                                ChatColor.DARK_RED + "Bandit" + ChatColor.GRAY + "."
                );
            } else {
                killer.sendMessage(ChatColor.YELLOW + "Bandit redemption progress: " +
                        ChatColor.GOLD + hunterKills + "/" + redemptionNeeded);
                killer.sendMessage(ChatColor.GRAY + "You need " +
                        ChatColor.RED + needed +
                        ChatColor.GRAY + " more bandit kills to lose your bandit status.");
            }
        } else {
            boolean becameHero = stats.handleHeroBanditKill(killerId);
            int heroKills = stats.getHeroBanditKills(killerId);
            int heroNeeded = ConfigValues.heroKills();
            int needed = Math.max(0, heroNeeded - heroKills);

            if (becameHero) {
                killer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD +
                        "You are now recognized as a HERO for slaying " + heroNeeded + " bandits!");

                Bukkit.broadcastMessage(
                        ChatColor.DARK_GRAY + "[" +
                                ChatColor.GOLD + "" + ChatColor.BOLD + "HERO" +
                                ChatColor.DARK_GRAY + "] " +
                                ChatColor.GOLD + killer.getName() +
                                ChatColor.GRAY + " has become a " +
                                ChatColor.GOLD + "" + ChatColor.BOLD + "HERO" +
                                ChatColor.GRAY + " by defeating " + heroNeeded + " bandits!"
                );
            } else if (!killerWasHero && !stats.isHero(killerId)) {
                killer.sendMessage(ChatColor.YELLOW + "Hero progress: " +
                        ChatColor.GOLD + heroKills + "/" + heroNeeded);
                killer.sendMessage(ChatColor.GRAY + "You need " +
                        ChatColor.GOLD + needed +
                        ChatColor.GRAY + " more bandit kills to become a Hero.");
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
            killer.sendMessage(
                    ChatColor.DARK_RED + "" + ChatColor.BOLD +
                    "You have fallen from HERO to BANDIT " +
                    ChatColor.RED + "for repeatedly preying on weaker players."
            );

            Bukkit.broadcastMessage(
                    ChatColor.DARK_GRAY + "[" +
                            ChatColor.DARK_RED + "" + ChatColor.BOLD + "DEMOTION" +
                            ChatColor.DARK_GRAY + "] " +
                            ChatColor.GOLD + killer.getName() +
                            ChatColor.GRAY + " has fallen from " +
                            ChatColor.GOLD + "" + ChatColor.BOLD + "HERO " +
                            ChatColor.GRAY + "to " +
                            ChatColor.DARK_RED + "" + ChatColor.BOLD + "BANDIT " +
                            ChatColor.GRAY + "after repeatedly attacking weaker players."
            );
        } else {
            killer.sendMessage(
                    ChatColor.DARK_RED + "" + ChatColor.BOLD +
                    "You are now a BANDIT. Your violent reputation precedes you..."
            );

            Bukkit.broadcastMessage(
                    ChatColor.DARK_GRAY + "[" +
                            ChatColor.DARK_RED + "" + ChatColor.BOLD + "BANDIT" +
                            ChatColor.DARK_GRAY + "] " +
                            ChatColor.RED + killer.getName() +
                            ChatColor.GRAY + " has become a " +
                            ChatColor.DARK_RED + "" + ChatColor.BOLD + "BANDIT" +
                            ChatColor.GRAY + " after repeatedly preying on weaker players."
            );
        }
    }

    private static String pickRandom(List<String> pool) {
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
