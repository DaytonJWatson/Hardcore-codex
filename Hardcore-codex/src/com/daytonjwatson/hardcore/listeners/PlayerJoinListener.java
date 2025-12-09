package com.daytonjwatson.hardcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.daytonjwatson.hardcore.config.ConfigValues;
import com.daytonjwatson.hardcore.managers.AuctionHouseManager;
import com.daytonjwatson.hardcore.managers.StatsManager;
import com.daytonjwatson.hardcore.utils.MessageStyler;
import com.daytonjwatson.hardcore.utils.TabUtil;
import com.daytonjwatson.hardcore.utils.TeleportUtil;
import com.daytonjwatson.hardcore.utils.Util;

public class PlayerJoinListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		StatsManager stats = StatsManager.get();
		
                if(player.hasPlayedBefore())
                        existingPlayer(player, event);
                else
                        newPlayer(player, event);

                if (ConfigValues.joinPanelEnabled()) {
                        MessageStyler.sendPanel(event.getPlayer(), ConfigValues.joinPanelTitle(),
                                ConfigValues.joinPanelLines().toArray(new String[0]));
                }

                stats.handleJoin(player);

                AuctionHouseManager auctionHouseManager = AuctionHouseManager.get();
                if (auctionHouseManager != null) {
                        auctionHouseManager.deliverPending(player);
                }

                TabUtil.updateTabForAll();
        }

        private void existingPlayer(Player player, PlayerJoinEvent event) {
                event.setJoinMessage(Util.color(ConfigValues.joinMessage(false).replace("%player%", player.getName())));
        }

        private void newPlayer(Player player, PlayerJoinEvent event) {
                event.setJoinMessage(Util.color(ConfigValues.joinMessage(true).replace("%player%", player.getName())));
                if (ConfigValues.randomSpawnEnabled()) {
                        TeleportUtil.randomSafeTeleportNearSpawn(player);
                }

                if (ConfigValues.giveGuideBookOnFirstJoin()) {
                        Util.giveHardcoreGuideBook(player);
                }
        }
}
